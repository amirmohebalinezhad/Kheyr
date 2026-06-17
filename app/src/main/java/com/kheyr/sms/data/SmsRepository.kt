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
    suspend fun loadThreads(): List<SmsThread> = withContext(Dispatchers.IO) {
        syncTelephonyMessages()
        smsDao.inboxThreads().map { it.toModel() }
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

    fun updatePinned(threadId: Long, pinned: Boolean, pinnedAt: Instant? = if (pinned) Instant.now() else null) =
        smsDao.updatePinned(threadId, pinned, pinnedAt)

    fun updateArchived(threadId: Long, archived: Boolean) = smsDao.updateArchived(threadId, archived)

    fun updateSpam(threadId: Long, spam: Boolean) = smsDao.updateSpam(threadId, spam)

    fun updateMuted(threadId: Long, muted: Boolean) = smsDao.updateMuted(threadId, muted)

    fun markThreadRead(threadId: Long) = smsDao.markThreadRead(threadId)

    fun updateSendStatus(messageId: Long, status: MessageStatus) = smsDao.updateSendStatus(messageId, status)

    private fun syncTelephonyMessages() {
        syncTelephonyMessages(newerThanId = smsDao.latestSyncedTelephonyId())
        smsDao.syncedTelephonyIds()
            .chunked(SYNC_REFRESH_BATCH_SIZE)
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
            val messages = mutableListOf<SmsMessageEntity>()
            while (cursor.moveToNext()) {
                val messageType = cursor.getInt(type)
                val direction = if (messageType == Telephony.Sms.MESSAGE_TYPE_SENT || messageType == Telephony.Sms.MESSAGE_TYPE_OUTBOX) {
                    MessageDirection.Outgoing
                } else {
                    MessageDirection.Incoming
                }
                val status = when (messageType) {
                    Telephony.Sms.MESSAGE_TYPE_SENT -> MessageStatus.Sent
                    Telephony.Sms.MESSAGE_TYPE_FAILED -> MessageStatus.Failed
                    Telephony.Sms.MESSAGE_TYPE_OUTBOX -> MessageStatus.Sending
                    else -> MessageStatus.Received
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

    fun deleteThreadMessages(threadId: Long) = smsDao.deleteThreadMessages(threadId)

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

    suspend fun markSending(messageId: Long) = withContext(Dispatchers.IO) { updateMessage(messageId, Telephony.Sms.MESSAGE_TYPE_OUTBOX, Telephony.Sms.STATUS_PENDING) }

    fun markSent(messageId: Long) = updateMessage(messageId, Telephony.Sms.MESSAGE_TYPE_SENT, Telephony.Sms.STATUS_NONE)

    fun markDelivered(messageId: Long) = updateMessage(messageId, Telephony.Sms.MESSAGE_TYPE_SENT, Telephony.Sms.STATUS_COMPLETE)

    fun markFailed(messageId: Long) = updateMessage(messageId, Telephony.Sms.MESSAGE_TYPE_FAILED, Telephony.Sms.STATUS_FAILED)

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
        private const val SYNC_REFRESH_BATCH_SIZE = 200
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
