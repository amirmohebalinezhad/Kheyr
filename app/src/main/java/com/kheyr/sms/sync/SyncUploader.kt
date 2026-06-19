package com.kheyr.sms.sync

import com.kheyr.sms.data.SmsMessage
import com.kheyr.sms.sync.crypto.SmsBodyEncryptor

interface SyncQueueStore {
    fun pendingRecords(limit: Int = 100): List<SyncQueueRecord>
    fun markUploaded(queueIds: List<Long>)

    /**
     * Removes successfully uploaded rows so the encrypted queue stays bounded and does not retain
     * plaintext payloads after upload. Defaults to [markUploaded] for stores that cannot delete.
     */
    fun deleteUploaded(queueIds: List<Long>) = markUploaded(queueIds)
}

interface SyncApiClient {
    fun upload(payloads: List<SyncUploadDto>)
}

fun interface SyncLogger {
    fun info(message: String)
}

class SyncUploader(
    private val settingsProvider: () -> SyncSettings,
    private val queueStore: SyncQueueStore,
    private val apiClient: SyncApiClient,
    private val encryptor: SmsBodyEncryptor,
    private val logger: SyncLogger = SyncLogger { },
) {
    fun uploadPending(limit: Int = 100): Int {
        val settings = settingsProvider()
        if (!settings.canUpload) {
            logger.info("Sync upload skipped because sync is disabled or device is unpaired")
            return 0
        }

        // EncryptedFieldPolicy marks "address" as protected, so the recipient number is salted-hashed
        // (never sent in cleartext). The device id is a stable per-install salt source.
        val addressHasher = PhoneIdentifierHasher(settings.deviceId!!)

        val records = queueStore.pendingRecords(limit)
        val skippedDeletedBackfillIds = records
            .filterIsInstance<InitialBackfillSyncRecord>()
            .filter { it.locallyDeletedBeforeSync }
            .map { it.queueId }
        val payloads = records.mapNotNull { toUploadDto(it, addressHasher) }
        if (payloads.isEmpty()) {
            // Nothing to upload, but skipped pre-sync deletions still occupy rows; drop them.
            if (skippedDeletedBackfillIds.isNotEmpty()) queueStore.deleteUploaded(skippedDeletedBackfillIds)
            return 0
        }

        apiClient.upload(payloads)
        // Delete only after a confirmed successful upload so retry semantics (pending() returns
        // rows that are still present) keep working when upload fails.
        queueStore.deleteUploaded(payloads.map { it.queueId } + skippedDeletedBackfillIds)
        logger.info("Uploaded ${payloads.size} encrypted sync event(s)")
        return payloads.size
    }

    private fun toUploadDto(record: SyncQueueRecord, addressHasher: PhoneIdentifierHasher): SyncUploadDto? = when (record) {
        is InitialBackfillSyncRecord -> {
            if (record.locallyDeletedBeforeSync) null else encryptedMessage(record.queueId, record.message, addressHasher)
        }
        is MessageChangeSyncRecord -> encryptedMessage(record.queueId, record.message, addressHasher)
        is DeleteEventSyncRecord -> SyncUploadDto(record.queueId, DeleteEventDto(record.messageId, record.deletedAt))
        is SpamStatusSyncRecord -> SyncUploadDto(record.queueId, SpamStatusDto(record.threadId, record.isSpam))
        is PinnedStatusSyncRecord -> SyncUploadDto(record.queueId, PinnedStatusDto(record.threadId, record.isPinned, record.pinnedAt))
        is ArchiveStatusSyncRecord -> SyncUploadDto(record.queueId, ArchiveStatusDto(record.threadId, record.isArchived))
        is NotificationSettingsSyncRecord -> SyncUploadDto(
            record.queueId,
            NotificationSettingsDto(
                threadId = record.settings.threadId,
                muted = record.settings.muted,
                customRingtoneUri = record.settings.customRingtoneUri,
            ),
        )
    }

    private fun encryptedMessage(queueId: Long, message: SmsMessage, addressHasher: PhoneIdentifierHasher): SyncUploadDto = SyncUploadDto(
        queueId = queueId,
        event = EncryptedSmsMessageDto(
            messageId = message.id,
            threadId = message.threadId,
            hashedAddress = addressHasher.hash(message.address),
            encryptedBody = encryptor.encryptBody(message.body),
            timestamp = message.timestamp,
            direction = message.direction,
            status = message.status,
            simSlot = message.simSlot,
            isSpam = message.isSpam,
        ),
    )
}
