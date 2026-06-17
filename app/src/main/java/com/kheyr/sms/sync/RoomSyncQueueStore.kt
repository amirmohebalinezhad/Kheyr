package com.kheyr.sms.sync

import com.kheyr.sms.data.MessageDirection
import com.kheyr.sms.data.MessageStatus
import com.kheyr.sms.data.SmsMessage
import com.kheyr.sms.data.SyncQueueDao
import com.kheyr.sms.data.SyncQueueEntity
import com.kheyr.sms.settings.ThreadNotificationSettings
import com.kheyr.sms.sync.crypto.EncryptedSmsBody
import org.json.JSONObject
import java.time.Instant

class RoomSyncQueueStore(private val dao: SyncQueueDao) : SyncQueueStore {
    override fun pendingRecords(limit: Int): List<SyncQueueRecord> = dao.pending(limit).mapNotNull(::decode)

    override fun markUploaded(queueIds: List<Long>) {
        if (queueIds.isNotEmpty()) dao.markUploaded(queueIds)
    }

    fun enqueueMessage(message: SmsMessage, initialBackfill: Boolean = false, locallyDeletedBeforeSync: Boolean = false) {
        val type = if (initialBackfill) "initial_backfill" else "message_change"
        dao.insert(
            SyncQueueEntity(
                eventType = type,
                payloadJson = messageJson(message, locallyDeletedBeforeSync).toString(),
                createdAt = Instant.now(),
            ),
        )
    }

    fun enqueueThreadState(threadId: Long, eventType: String, payload: JSONObject) {
        dao.insert(
            SyncQueueEntity(
                eventType = eventType,
                payloadJson = payload.put("thread_id", threadId).toString(),
                createdAt = Instant.now(),
            ),
        )
    }

    private fun decode(entity: SyncQueueEntity): SyncQueueRecord? = runCatching {
        val json = JSONObject(entity.payloadJson)
        when (entity.eventType) {
            "initial_backfill" -> InitialBackfillSyncRecord(
                queueId = entity.id,
                createdAt = entity.createdAt,
                message = parseMessage(json),
                locallyDeletedBeforeSync = json.optBoolean("locally_deleted"),
            )
            "message_change" -> MessageChangeSyncRecord(entity.id, entity.createdAt, parseMessage(json))
            "delete" -> DeleteEventSyncRecord(entity.id, entity.createdAt, json.getLong("message_id"), Instant.parse(json.getString("deleted_at")))
            "spam" -> SpamStatusSyncRecord(entity.id, entity.createdAt, json.getLong("thread_id"), json.getBoolean("is_spam"))
            "pin" -> PinnedStatusSyncRecord(
                entity.id,
                entity.createdAt,
                json.getLong("thread_id"),
                json.getBoolean("is_pinned"),
                json.optString("pinned_at").takeIf { it.isNotBlank() }?.let(Instant::parse),
            )
            "archive" -> ArchiveStatusSyncRecord(entity.id, entity.createdAt, json.getLong("thread_id"), json.getBoolean("is_archived"))
            "notification" -> NotificationSettingsSyncRecord(
                entity.id,
                entity.createdAt,
                ThreadNotificationSettings(
                    threadId = json.getLong("thread_id"),
                    muted = json.getBoolean("muted"),
                    customRingtoneUri = json.optString("custom_ringtone").takeIf { it.isNotBlank() },
                ),
            )
            else -> null
        }
    }.getOrNull()

    private fun messageJson(message: SmsMessage, locallyDeleted: Boolean): JSONObject = JSONObject().apply {
        put("id", message.id)
        put("thread_id", message.threadId)
        put("address", message.address)
        put("body", message.body)
        put("timestamp", message.timestamp.toString())
        put("direction", message.direction.name)
        put("status", message.status.name)
        put("sim_slot", message.simSlot)
        put("is_spam", message.isSpam)
        put("locally_deleted", locallyDeleted)
    }

    private fun parseMessage(json: JSONObject): SmsMessage = SmsMessage(
        id = json.getLong("id"),
        threadId = json.getLong("thread_id"),
        address = json.getString("address"),
        body = json.getString("body"),
        timestamp = Instant.parse(json.getString("timestamp")),
        direction = MessageDirection.valueOf(json.getString("direction")),
        status = MessageStatus.valueOf(json.getString("status")),
        simSlot = json.optInt("sim_slot").takeIf { json.has("sim_slot") && !json.isNull("sim_slot") },
        isSpam = json.optBoolean("is_spam"),
    )
}
