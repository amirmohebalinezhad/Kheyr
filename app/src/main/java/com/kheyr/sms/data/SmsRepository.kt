package com.kheyr.sms.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import java.time.Instant

class SmsRepository(private val context: Context) {
    fun loadThreads(): List<SmsThread> {
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
        return context.contentResolver.query(Telephony.Sms.CONTENT_URI, projection, null, null, "date DESC")?.use { cursor ->
            val latestByThread = linkedMapOf<Long, SmsThread>()
            val thread = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val address = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val read = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
            val subId = cursor.getColumnIndex(SUBSCRIPTION_ID)
            while (cursor.moveToNext()) {
                val threadId = cursor.getLong(thread)
                val previous = latestByThread[threadId]
                val unread = if (cursor.getInt(read) == 0) 1 else 0
                if (previous == null) {
                    latestByThread[threadId] = SmsThread(
                        id = threadId,
                        address = cursor.getString(address).orEmpty(),
                        displayName = cursor.getString(address).orEmpty(),
                        lastMessage = cursor.getString(body).orEmpty(),
                        lastMessageAt = Instant.ofEpochMilli(cursor.getLong(date)),
                        unreadCount = unread,
                        simSlot = cursor.intOrNull(subId),
                    )
                } else if (unread > 0) {
                    latestByThread[threadId] = previous.copy(unreadCount = previous.unreadCount + unread)
                }
            }
            latestByThread.values.toList()
        }.orEmpty()
    }

    fun loadMessages(threadId: Long): List<SmsMessage> {
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
        return context.contentResolver.query(
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

    fun persistOutgoing(recipient: String, body: String, subscriptionId: Int?): Long {
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
        return context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)?.let(ContentUris::parseId)
            ?: error("Unable to persist outgoing SMS")
    }

    fun markSending(messageId: Long) = updateMessage(messageId, Telephony.Sms.MESSAGE_TYPE_OUTBOX, Telephony.Sms.STATUS_PENDING)

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
    }
}
