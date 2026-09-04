package com.kheyr.sms.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kheyr.sms.preferences.AppPreferences
import com.kheyr.sms.sync.ArchiveStatusSyncRecord
import com.kheyr.sms.sync.DeleteEventSyncRecord
import com.kheyr.sms.sync.InitialBackfillSyncRecord
import com.kheyr.sms.sync.MessageChangeSyncRecord
import com.kheyr.sms.sync.NotificationSettingsSyncRecord
import com.kheyr.sms.sync.PinnedStatusSyncRecord
import com.kheyr.sms.sync.RoomSyncQueueStore
import com.kheyr.sms.sync.SpamStatusSyncRecord
import com.kheyr.sms.sync.SyncSettings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/** Covers the sync_queue feed added for B-03 and the retention helper added for B-18. */
@RunWith(RobolectricTestRunner::class)
class SmsRepositorySyncQueueTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var dao: SmsDao
    private lateinit var queue: RoomSyncQueueStore
    private lateinit var repository: SmsRepository

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.smsDao()
        queue = RoomSyncQueueStore(database.syncQueueDao())
        repository = SmsRepository(context, dao, queue)
    }

    @After fun tearDown() {
        database.close()
    }

    private fun enableSync() {
        AppPreferences(context).saveSyncSettings(SyncSettings(enabled = true, deviceId = "device-1"))
    }

    private fun pending() = queue.pendingRecords(100)

    @Test fun nothingIsQueuedWhileSyncIsDisabled() = runBlocking {
        val messageId = repository.insertIncomingSms(threadId = 1, address = "+989120000000", body = "hello")
        repository.updatePinned(1, true)
        repository.updateArchived(1, true)
        repository.updateSpam(1, true)
        repository.updateMuted(1, true)
        repository.autoMarkSpam(1)
        // Asserted while the message still exists, so the 0 means "sync is off", not "nothing to queue".
        assertEquals(0, repository.enqueueInitialBackfill())
        repository.deleteLocalMessagesByIds(listOf(messageId))

        assertEquals(emptyList<Any>(), pending())
    }

    @Test fun insertedMessageIsQueuedAndDecodesBack() {
        enableSync()

        repository.insertIncomingSms(threadId = 7, address = "+989120000000", body = "salam", timestamp = Instant.parse("2026-01-01T00:00:00Z"))

        val record = pending().single() as MessageChangeSyncRecord
        assertEquals(7L, record.message.threadId)
        assertEquals("+989120000000", record.message.address)
        assertEquals("salam", record.message.body)
        assertEquals(MessageDirection.Incoming, record.message.direction)
        assertEquals(MessageStatus.Received, record.message.status)
    }

    @Test fun outgoingSendStatusTransitionsAreQueued() {
        enableSync()

        val messageId = repository.insertOutgoingSms(threadId = 7, address = "+989120000000", body = "hi")
        repository.updateSendStatus(messageId, MessageStatus.Sent)

        val statuses = pending().filterIsInstance<MessageChangeSyncRecord>().map { it.message.status }
        assertEquals(listOf(MessageStatus.Sending, MessageStatus.Sent), statuses)
    }

    @Test fun threadStateChangesAreQueuedWithDecodablePayloads() = runBlocking {
        enableSync()
        repository.insertIncomingSms(threadId = 3, address = "+989120000000", body = "hello")

        repository.updatePinned(3, true, Instant.parse("2026-02-01T00:00:00Z"))
        repository.updateArchived(3, true)
        repository.updateSpam(3, true)
        repository.updateMuted(3, true)

        val records = pending().drop(1)
        val pin = records[0] as PinnedStatusSyncRecord
        assertEquals(3L, pin.threadId)
        assertTrue(pin.isPinned)
        assertEquals(Instant.parse("2026-02-01T00:00:00Z"), pin.pinnedAt)
        assertTrue((records[1] as ArchiveStatusSyncRecord).isArchived)
        assertTrue((records[2] as SpamStatusSyncRecord).isSpam)
        assertTrue((records[3] as NotificationSettingsSyncRecord).settings.muted)
    }

    @Test fun deletingLocalMessagesQueuesADeleteEvent() = runBlocking {
        enableSync()
        val messageId = repository.insertIncomingSms(threadId = 4, address = "+989120000000", body = "hello")

        repository.deleteLocalMessagesByIds(listOf(messageId))

        val delete = pending().filterIsInstance<DeleteEventSyncRecord>().single()
        assertEquals(messageId, delete.messageId)
        assertTrue(dao.messagesForThread(4).isEmpty())
    }

    @Test fun initialBackfillQueuesEveryLocalMessage() = runBlocking {
        dao.insertIncomingSms(entity(threadId = 1, body = "one"))
        dao.insertIncomingSms(entity(threadId = 2, body = "two"))
        enableSync()

        assertEquals(2, repository.enqueueInitialBackfill())

        val bodies = pending().filterIsInstance<InitialBackfillSyncRecord>().map { it.message.body }
        assertEquals(listOf("one", "two"), bodies)
    }

    @Test fun autoMarkSpamRespectsAUserNotSpamCorrectionAndQueuesNothingForIt() {
        enableSync()
        repository.insertIncomingSms(threadId = 9, address = "+989120000000", body = "prize")
        dao.updateSpam(9, false)

        repository.autoMarkSpam(9)

        assertEquals(false, dao.isThreadSpam(9))
        assertTrue(pending().none { it is SpamStatusSyncRecord })
    }

    @Test fun deleteSpamOlderThanNeverDeletesWhenRetentionIsZero() = runBlocking {
        dao.insertIncomingSms(entity(threadId = 1, body = "old spam", at = Instant.parse("2020-01-01T00:00:00Z")))
        dao.updateSpam(1, true)

        assertEquals(0, repository.deleteSpamOlderThan(0))
        assertEquals(1, dao.messagesForThread(1).size)
    }

    @Test fun deleteSpamOlderThanDeletesOnlyOldSpamMessages() = runBlocking {
        dao.insertIncomingSms(entity(threadId = 1, body = "old spam", at = Instant.parse("2020-01-01T00:00:00Z")))
        dao.insertIncomingSms(entity(threadId = 1, body = "fresh spam", at = Instant.now()))
        dao.insertIncomingSms(entity(threadId = 2, body = "old but not spam", at = Instant.parse("2020-01-01T00:00:00Z")))
        dao.updateSpam(1, true)

        assertEquals(1, repository.deleteSpamOlderThan(30))

        assertEquals(listOf("fresh spam"), dao.messagesForThread(1).map { it.body })
        assertEquals(1, dao.messagesForThread(2).size)
    }

    private fun entity(threadId: Long, body: String, at: Instant = Instant.parse("2026-01-01T00:00:00Z")) = SmsMessageEntity(
        threadId = threadId,
        address = "+989120000000",
        body = body,
        timestamp = at,
        direction = MessageDirection.Incoming,
        status = MessageStatus.Received,
        read = false,
    )
}
