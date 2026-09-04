package com.kheyr.sms.sync

import com.kheyr.sms.data.MessageDirection
import com.kheyr.sms.data.MessageStatus
import com.kheyr.sms.data.SmsMessage
import com.kheyr.sms.sync.crypto.SmsBodyEncryptor
import java.time.Instant
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncRetryPolicyTest {
    private val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")

    @Test fun backoffDoublesPerAttemptAndSaturatesAtTheCap() {
        assertEquals(1_000L, SyncRetryPolicy.delayMillis(0))
        assertEquals(2_000L, SyncRetryPolicy.delayMillis(1))
        assertEquals(32_000L, SyncRetryPolicy.delayMillis(5))
        // Attempt is clamped at 6 (64s), so every later attempt sits on the 60s ceiling.
        assertEquals(60_000L, SyncRetryPolicy.delayMillis(6))
        assertEquals(60_000L, SyncRetryPolicy.delayMillis(50))
    }

    @Test fun eventsSurviveAnOfflineRunAndUploadOnTheNextOne() {
        val store = MutableQueueStore(
            mutableListOf(
                MessageChangeSyncRecord(
                    queueId = 1,
                    createdAt = Instant.EPOCH,
                    message = SmsMessage(
                        id = 7,
                        threadId = 99,
                        address = "+15551234567",
                        body = "queued while offline",
                        timestamp = Instant.ofEpochMilli(1234),
                        direction = MessageDirection.Incoming,
                        status = MessageStatus.Received,
                    ),
                ),
            ),
        )
        val api = FlakyApiClient(failuresBeforeSuccess = 1)
        val uploader = SyncUploader(
            settingsProvider = { SyncSettings(enabled = true, deviceId = "device-1") },
            queueStore = store,
            apiClient = api,
            encryptor = SmsBodyEncryptor(key),
        )

        assertEquals(0, uploader.uploadPending())
        assertEquals(listOf(1L), store.pendingRecords().map { it.queueId })

        assertEquals(1, uploader.uploadPending())
        assertEquals(emptyList<Long>(), store.pendingRecords().map { it.queueId })
        assertEquals(2, api.attempts)
    }

    private class FlakyApiClient(private var failuresBeforeSuccess: Int) : SyncApiClient {
        var attempts = 0
        override fun upload(payloads: List<SyncUploadDto>): Boolean {
            attempts++
            if (failuresBeforeSuccess > 0) {
                failuresBeforeSuccess--
                return false
            }
            return true
        }
    }

    private class MutableQueueStore(private val records: MutableList<SyncQueueRecord>) : SyncQueueStore {
        override fun pendingRecords(limit: Int): List<SyncQueueRecord> = records.take(limit)
        override fun markUploaded(queueIds: List<Long>) {
            records.removeAll { it.queueId in queueIds }
        }
    }
}
