package com.kheyr.sms.data

import android.content.Context
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
        )
        return context.contentResolver.query(Telephony.Sms.CONTENT_URI, projection, null, null, "date DESC")?.use { cursor ->
            val latestByThread = linkedMapOf<Long, SmsThread>()
            val thread = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val address = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val read = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
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
                    )
                } else if (unread > 0) {
                    latestByThread[threadId] = previous.copy(unreadCount = previous.unreadCount + unread)
                }
            }
            latestByThread.values.toList()
        }.orEmpty()
    }
}
