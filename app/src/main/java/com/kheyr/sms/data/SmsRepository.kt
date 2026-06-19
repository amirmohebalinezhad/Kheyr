package com.kheyr.sms.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class SmsRepository(
    private val context: Context,
    private val smsDao: SmsDao = AppDatabase.getInstance(context).smsDao(),
) {
    suspend fun loadLocalThreads(): List<SmsThread> = withContext(Dispatchers.IO) {
        smsDao.inboxThreads().map { it.toModel() }
    }

    suspend fun syncTelephonyMessages() = withContext(Dispatchers.IO) {
        syncNewTelephonyMessages()
        refreshRecentOutgoingMessages()
    }

    suspend fun syncTelephonyMessagesByIds(telephonyIds: List<Long>) = withContext(Dispatchers.IO) {
        if (telephonyIds.isEmpty()) return@withContext
        syncTelephonyMessages(telephonyIds = telephonyIds)
    }

    suspend fun loadPinnedThreads(): List<SmsThread> = withContext(Dispatchers.IO) { smsDao.pinnedThreads().map { it.toModel() } }

    suspend fun loadSpamThreads(): List<SmsThread> = withContext(Dispatchers.IO) { smsDao.spamThreads().map { it.toModel() } }

    suspend fun loadArchivedThreads(): List<SmsThread> = withContext(Dispatchers.IO) { smsDao.archivedThreads().map { it.toModel() } }

    fun insertIncomingSms(
        threadId: Long,
        address: String,
        body: String,
        timestamp: Instant = Instant.now(),
        telephonyId: Long? = null,
        read: Boolean = false,
        simSlot: Int? = null,
    ): Long = smsDao.insertIncomingSms(
        SmsMessageEntity(
            telephonyId = telephonyId,
            threadId = threadId,
            address = address,
            body = body,
            timestamp = timestamp,
            direction = MessageDirection.Incoming,
            status = MessageStatus.Received,
            read = read,
            simSlot = simSlot,
        )
    )

    fun insertOutgoingSms(
        threadId: Long,
        address: String,
        body: String,
        timestamp: Instant = Instant.now(),
        status: MessageStatus = MessageStatus.Sending,
        simSlot: Int? = null,
    ): Long = smsDao.insertOutgoingSms(
        SmsMessageEntity(
            threadId = threadId,
            address = address,
            body = body,
            timestamp = timestamp,
            direction = MessageDirection.Outgoing,
            status = status,
            read = true,
            simSlot = simSlot,
        )
    )

    suspend fun updatePinned(threadId: Long, pinned: Boolean, pinnedAt: Instant? = if (pinned) Instant.now() else null) =
        withContext(Dispatchers.IO) { smsDao.updatePinned(threadId, pinned, pinnedAt) }

    suspend fun updateArchived(threadId: Long, archived: Boolean) =
        withContext(Dispatchers.IO) { smsDao.updateArchived(threadId, archived) }

    suspend fun updateSpam(threadId: Long, spam: Boolean) =
        withContext(Dispatchers.IO) { smsDao.updateSpam(threadId, spam) }

    suspend fun updateMuted(threadId: Long, muted: Boolean) =
        withContext(Dispatchers.IO) { smsDao.updateMuted(threadId, muted) }

    suspend fun markThreadRead(threadId: Long) =
        withContext(Dispatchers.IO) { smsDao.markThreadRead(threadId) }

    fun updateSendStatus(messageId: Long, status: MessageStatus) = smsDao.updateSendStatus(messageId, status)

    private fun syncNewTelephonyMessages() {
        syncTelephonyMessages(newerThanId = smsDao.latestSyncedTelephonyId())
    }

    private fun refreshRecentOutgoingMessages() {
        smsDao.recentOutgoingTelephonyIds(RECENT_OUTGOING_REFRESH_LIMIT)
            .chunked(RECENT_OUTGOING_REFRESH_BATCH_SIZE)
            .forEach { ids -> syncTelephonyMessages(telephonyIds = ids) }
    }

    private fun syncTelephonyMessages(newerThanId: Long? = null, telephonyIds: List<Long>? = null) {
        val selection: String
        val selectionArgs: Array<String>
        when {
            telephonyIds != null -> {
                if (telephonyIds.isEmpty()) return
                val placeholders = telephonyIds.joinToString(",") { "?" }
                selection = "${Telephony.Sms._ID} IN ($placeholders)"
                selectionArgs = telephonyIds.map(Long::toString).toTypedArray()
            }
            newerThanId != null -> {
                selection = "${Telephony.Sms._ID} > ?"
                selectionArgs = arrayOf(newerThanId.toString())
            }
            else -> return
        }
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE,
            Telephony.Sms.STATUS,
            SUBSCRIPTION_ID,
        )
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${Telephony.Sms._ID} ASC",
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val thread = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val address = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val read = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
            val subId = cursor.getColumnIndex(SUBSCRIPTION_ID)
            val type = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val statusColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.STATUS)
            val messages = mutableListOf<SmsMessageEntity>()
            while (cursor.moveToNext()) {
                val messageType = cursor.getInt(type)
                val direction = TelephonyDirectionMapper.directionFromType(messageType)
                val status = if (direction == MessageDirection.Outgoing) {
                    messageStatus(messageType, cursor.getInt(statusColumn))
                } else {
                    MessageStatus.Received
                }
                messages += SmsMessageEntity(
                    telephonyId = cursor.getLong(id),
                    threadId = cursor.getLong(thread),
                    address = cursor.getString(address).orEmpty(),
                    simSlot = cursor.intOrNull(subId),
                    body = cursor.getString(body).orEmpty(),
                    timestamp = Instant.ofEpochMilli(cursor.getLong(date)),
                    direction = direction,
                    status = status,
                    read = cursor.getInt(read) != 0 || direction == MessageDirection.Outgoing,
                )
                if (messages.size == SYNC_INSERT_BATCH_SIZE) {
                    smsDao.upsertTelephonyMessageBatch(messages)
                    messages.clear()
                }
            }
            if (messages.isNotEmpty()) {
                smsDao.upsertTelephonyMessageBatch(messages)
            }
        }
    }

    suspend fun recentOutgoingThreadId(address: String, body: String, withinSeconds: Long = 120): Long? = withContext(Dispatchers.IO) {
        smsDao.recentOutgoingThreadId(address, body, Instant.now().minusSeconds(withinSeconds))
    }

    suspend fun deleteThreadMessages(threadId: Long) =
        withContext(Dispatchers.IO) { smsDao.deleteThreadMessages(threadId) }

    suspend fun deleteMessagesByIds(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        smsDao.deleteMessagesByIds(ids)
    }

    suspend fun loadLocalMessageEntities(threadId: Long): List<SmsMessageEntity> = withContext(Dispatchers.IO) {
        smsDao.messagesForThread(threadId)
    }

    suspend fun restoreThreadMessages(messages: List<SmsMessageEntity>) = withContext(Dispatchers.IO) {
        if (messages.isEmpty()) return@withContext
        smsDao.insertSmsBatch(messages.map { it.copy(id = 0) })
    }

    suspend fun loadLocalMessages(threadId: Long): List<SmsMessage> = withContext(Dispatchers.IO) {
        smsDao.messagesForThread(threadId).map { it.toModel() }
    }

    suspend fun searchLocalMessages(query: String): List<SmsMessage> = withContext(Dispatchers.IO) {
        smsDao.searchMessages(query).map { it.toModel() }
    }

    suspend fun loadFailedOutgoingMessages(): List<SmsMessage> = withContext(Dispatchers.IO) {
        smsDao.failedOutgoingMessages().map { it.toModel() }
    }

    suspend fun loadMessages(threadId: Long): List<SmsMessage> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.STATUS,
            SUBSCRIPTION_ID,
        )
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "date ASC",
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val thread = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val address = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val type = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val status = cursor.getColumnIndexOrThrow(Telephony.Sms.STATUS)
            val subId = cursor.getColumnIndex(SUBSCRIPTION_ID)
            buildList {
                while (cursor.moveToNext()) {
                    val smsType = cursor.getInt(type)
                    add(
                        SmsMessage(
                            id = cursor.getLong(id),
                            threadId = cursor.getLong(thread),
                            address = cursor.getString(address).orEmpty(),
                            body = cursor.getString(body).orEmpty(),
                            timestamp = Instant.ofEpochMilli(cursor.getLong(date)),
                            direction = if (smsType == Telephony.Sms.MESSAGE_TYPE_INBOX) MessageDirection.Incoming else MessageDirection.Outgoing,
                            status = messageStatus(smsType, cursor.getInt(status)),
                            simSlot = cursor.intOrNull(subId),
                        ),
                    )
                }
            }
        }.orEmpty()
    }

    suspend fun persistOutgoing(recipient: String, body: String, subscriptionId: Int?): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, recipient)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_OUTBOX)
            put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_PENDING)
            subscriptionId?.let { put(SUBSCRIPTION_ID, it) }
        }
        context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)?.let(ContentUris::parseId)
            ?: error("Unable to persist outgoing SMS")
    }

    suspend fun markSending(telephonyId: Long) = withContext(Dispatchers.IO) {
        updateMessage(telephonyId, Telephony.Sms.MESSAGE_TYPE_OUTBOX, Telephony.Sms.STATUS_PENDING)
        smsDao.updateSendStatusByTelephonyId(telephonyId, MessageStatus.Sending)
    }

    fun markSent(telephonyId: Long) {
        updateMessage(telephonyId, Telephony.Sms.MESSAGE_TYPE_SENT, Telephony.Sms.STATUS_NONE)
        smsDao.updateSendStatusByTelephonyId(telephonyId, MessageStatus.Sent)
    }

    fun markDelivered(telephonyId: Long) {
        updateMessage(telephonyId, Telephony.Sms.MESSAGE_TYPE_SENT, Telephony.Sms.STATUS_COMPLETE)
        smsDao.updateSendStatusByTelephonyId(telephonyId, MessageStatus.Delivered)
    }

    fun markFailed(telephonyId: Long) {
        updateMessage(telephonyId, Telephony.Sms.MESSAGE_TYPE_FAILED, Telephony.Sms.STATUS_FAILED)
        smsDao.updateSendStatusByTelephonyId(telephonyId, MessageStatus.Failed)
    }

    fun notifyRefreshForTelephonyId(telephonyId: Long) {
        smsDao.messageByTelephonyId(telephonyId)?.threadId?.let { SmsRefreshEvents.notifyThreadChanged(it) }
    }

    private fun updateMessage(messageId: Long, type: Int, status: Int) {
        val values = ContentValues().apply {
            put(Telephony.Sms.TYPE, type)
            put(Telephony.Sms.STATUS, status)
        }
        context.contentResolver.update(messageUri(messageId), values, null, null)
    }

    private fun messageStatus(type: Int, status: Int): MessageStatus = when (type) {
        Telephony.Sms.MESSAGE_TYPE_INBOX -> MessageStatus.Received
        Telephony.Sms.MESSAGE_TYPE_OUTBOX, Telephony.Sms.MESSAGE_TYPE_QUEUED -> MessageStatus.Sending
        Telephony.Sms.MESSAGE_TYPE_FAILED -> MessageStatus.Failed
        Telephony.Sms.MESSAGE_TYPE_SENT -> if (status == Telephony.Sms.STATUS_COMPLETE) MessageStatus.Delivered else MessageStatus.Sent
        else -> MessageStatus.Received
    }

    private fun messageUri(messageId: Long): Uri = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, messageId)

    private fun android.database.Cursor.intOrNull(column: Int): Int? = if (column >= 0 && !isNull(column)) getInt(column) else null

    companion object {
        private const val SUBSCRIPTION_ID = "sub_id"
        private const val SYNC_INSERT_BATCH_SIZE = 500
        private const val RECENT_OUTGOING_REFRESH_LIMIT = 50
        private const val RECENT_OUTGOING_REFRESH_BATCH_SIZE = 25
    }
    private fun SmsMessageEntity.toModel() = SmsMessage(
        id = id,
        threadId = threadId,
        address = address,
        body = body,
        timestamp = timestamp,
        direction = direction,
        status = status,
        simSlot = simSlot,
        telephonyId = telephonyId,
    )

    private fun ThreadWithLatestMessage.toModel() = SmsThread(
        id = id,
        address = address,
        displayName = displayName,
        lastMessage = lastMessage,
        lastMessageAt = lastMessageAt,
        unreadCount = unreadCount,
        isPinned = isPinned,
        pinnedAt = pinnedAt,
        isMuted = isMuted,
        isSpam = isSpam,
        isArchived = isArchived,
        simSlot = simSlot,
    )
}
