package com.kheyr.sms.sync

import com.kheyr.sms.data.MessageDirection
import com.kheyr.sms.data.MessageStatus
import com.kheyr.sms.data.SmsMessage
import com.kheyr.sms.settings.ThreadNotificationSettings
import com.kheyr.sms.sync.crypto.EncryptedSmsBody
import java.time.Instant

/** Locally persisted queue rows awaiting sync upload. */
sealed interface SyncQueueRecord {
    val queueId: Long
    val createdAt: Instant
}

data class InitialBackfillSyncRecord(
    override val queueId: Long,
    override val createdAt: Instant,
    val message: SmsMessage,
    val locallyDeletedBeforeSync: Boolean = false,
) : SyncQueueRecord

data class MessageChangeSyncRecord(
    override val queueId: Long,
    override val createdAt: Instant,
    val message: SmsMessage,
) : SyncQueueRecord

data class DeleteEventSyncRecord(
    override val queueId: Long,
    override val createdAt: Instant,
    val messageId: Long,
    val deletedAt: Instant,
) : SyncQueueRecord

data class SpamStatusSyncRecord(
    override val queueId: Long,
    override val createdAt: Instant,
    val threadId: Long,
    val isSpam: Boolean,
) : SyncQueueRecord

data class PinnedStatusSyncRecord(
    override val queueId: Long,
    override val createdAt: Instant,
    val threadId: Long,
    val isPinned: Boolean,
    val pinnedAt: Instant?,
) : SyncQueueRecord

data class ArchiveStatusSyncRecord(
    override val queueId: Long,
    override val createdAt: Instant,
    val threadId: Long,
    val isArchived: Boolean,
) : SyncQueueRecord

data class NotificationSettingsSyncRecord(
    override val queueId: Long,
    override val createdAt: Instant,
    val settings: ThreadNotificationSettings,
) : SyncQueueRecord

data class SyncUploadDto(
    val queueId: Long,
    val event: SyncEventDto,
)

sealed interface SyncEventDto

data class EncryptedSmsMessageDto(
    val messageId: Long,
    val threadId: Long,
    val address: String,
    val encryptedBody: EncryptedSmsBody,
    val timestamp: Instant,
    val direction: MessageDirection,
    val status: MessageStatus,
    val simSlot: Int?,
    val isSpam: Boolean,
) : SyncEventDto

data class DeleteEventDto(val messageId: Long, val deletedAt: Instant) : SyncEventDto

data class SpamStatusDto(val threadId: Long, val isSpam: Boolean) : SyncEventDto

data class PinnedStatusDto(val threadId: Long, val isPinned: Boolean, val pinnedAt: Instant?) : SyncEventDto

data class ArchiveStatusDto(val threadId: Long, val isArchived: Boolean) : SyncEventDto

data class NotificationSettingsDto(
    val threadId: Long,
    val muted: Boolean,
    val customRingtoneUri: String?,
) : SyncEventDto
