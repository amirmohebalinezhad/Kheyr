package com.kheyr.sms.sync

import com.kheyr.sms.data.MessageDirection
import com.kheyr.sms.data.MessageStatus
import com.kheyr.sms.data.SmsMessage
import com.kheyr.sms.sync.crypto.SmsBodyEncryptor
import java.time.Instant
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncUploaderTest {
    private val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")

    @Test fun plaintextSmsBodyIsNotPresentInUploadDtoOrLogs() {
        val plaintext = "Secret bank OTP 123456"
        val store = InMemoryQueueStore(
            listOf(
                MessageChangeSyncRecord(
                    queueId = 1,
                    createdAt = Instant.EPOCH,
                    message = sms(body = plaintext),
                ),
            ),
        )
        val api = CapturingApiClient()
        val logger = CapturingLogger()

        val uploaded = SyncUploader(
            settingsProvider = { SyncSettings(enabled = true, deviceId = "device-1") },
            queueStore = store,
            apiClient = api,
            encryptor = SmsBodyEncryptor(key),
            logger = logger,
        ).uploadPending()

        assertEquals(1, uploaded)
        val payloadString = api.uploaded.single().toString()
        assertFalse(payloadString.contains(plaintext))
        assertTrue(api.uploaded.single().event is EncryptedSmsMessageDto)
        assertFalse(logger.messages.joinToString("\n").contains(plaintext))
        assertEquals(listOf(1L), store.uploadedQueueIds)
    }

    @Test fun initialBackfillSkipsMessagesDeletedBeforeSyncWasEnabled() {
        val store = InMemoryQueueStore(
            listOf(
                InitialBackfillSyncRecord(
                    queueId = 1,
                    createdAt = Instant.EPOCH,
                    message = sms(id = 10, body = "old deleted body"),
                    locallyDeletedBeforeSync = true,
                ),
                InitialBackfillSyncRecord(
                    queueId = 2,
                    createdAt = Instant.EPOCH,
                    message = sms(id = 11, body = "current body"),
                ),
            ),
        )
        val api = CapturingApiClient()

        val uploaded = SyncUploader(
            settingsProvider = { SyncSettings(enabled = true, deviceId = "device-1") },
            queueStore = store,
            apiClient = api,
            encryptor = SmsBodyEncryptor(key),
        ).uploadPending()

        assertEquals(1, uploaded)
        assertEquals(2L, api.uploaded.single().queueId)
        assertFalse(api.uploaded.toString().contains("old deleted body"))
        assertEquals(listOf(2L, 1L), store.uploadedQueueIds)
    }

    @Test fun syncIsOffByDefault() {
        assertFalse(SyncSettings().canUpload)
    }

    private fun sms(id: Long = 7, body: String) = SmsMessage(
        id = id,
        threadId = 99,
        address = "+15551234567",
        body = body,
        timestamp = Instant.ofEpochMilli(1234),
        direction = MessageDirection.Incoming,
        status = MessageStatus.Received,
    )

    private class CapturingApiClient : SyncApiClient {
        val uploaded = mutableListOf<SyncUploadDto>()
        override fun upload(payloads: List<SyncUploadDto>) {
            uploaded += payloads
        }
    }

    private class CapturingLogger : SyncLogger {
        val messages = mutableListOf<String>()
        override fun info(message: String) {
            messages += message
        }
    }

    private class InMemoryQueueStore(private val records: List<SyncQueueRecord>) : SyncQueueStore {
        val uploadedQueueIds = mutableListOf<Long>()
        override fun pendingRecords(limit: Int): List<SyncQueueRecord> = records.take(limit)
        override fun markUploaded(queueIds: List<Long>) {
            uploadedQueueIds += queueIds
        }
    }
}
