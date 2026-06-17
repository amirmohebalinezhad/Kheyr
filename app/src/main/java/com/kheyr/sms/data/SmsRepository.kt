package com.kheyr.sms.data

import android.content.Context
import android.provider.Telephony
import java.time.Instant

class SmsRepository(
    private val context: Context,
    private val smsDao: SmsDao = AppDatabase.getInstance(context).smsDao(),
) {
    fun loadThreads(): List<SmsThread> {
        syncTelephonyMessages()
        return smsDao.inboxThreads().map { it.toModel() }
    }

    fun loadSpamThreads(): List<SmsThread> = smsDao.spamThreads().map { it.toModel() }

    fun loadArchivedThreads(): List<SmsThread> = smsDao.archivedThreads().map { it.toModel() }

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

    fun updateSendStatus(messageId: Long, status: MessageStatus) = smsDao.updateSendStatus(messageId, status)

    private fun syncTelephonyMessages() {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE,
        )
        context.contentResolver.query(Telephony.Sms.CONTENT_URI, projection, null, null, "date ASC")?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val thread = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val address = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val read = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
            val type = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
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
                smsDao.insertSms(
                    SmsMessageEntity(
                        telephonyId = cursor.getLong(id),
                        threadId = cursor.getLong(thread),
                        address = cursor.getString(address).orEmpty(),
                        body = cursor.getString(body).orEmpty(),
                        timestamp = Instant.ofEpochMilli(cursor.getLong(date)),
                        direction = direction,
                        status = status,
                        read = cursor.getInt(read) != 0 || direction == MessageDirection.Outgoing,
                    )
                )
            }
        }
    }

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
