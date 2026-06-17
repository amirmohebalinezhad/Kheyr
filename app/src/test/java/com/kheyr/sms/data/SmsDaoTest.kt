package com.kheyr.sms.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class SmsDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: SmsDao

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.smsDao()
    }

    @After fun tearDown() {
        database.close()
    }

    @Test fun insertGroupsMessagesByThreadWithLatestPreviewAndUnreadCount() {
        dao.insertIncomingSms(message(threadId = 1, body = "older", at = "2026-01-01T00:00:00Z", read = false))
        dao.insertIncomingSms(message(threadId = 1, body = "newer", at = "2026-01-02T00:00:00Z", read = true))
        dao.insertOutgoingSms(message(threadId = 2, body = "outgoing", at = "2026-01-03T00:00:00Z", direction = MessageDirection.Outgoing, status = MessageStatus.Sent, read = true))

        val threads = dao.inboxThreads()

        assertEquals(listOf(2L, 1L), threads.map { it.id })
        assertEquals("newer", threads.single { it.id == 1L }.lastMessage)
        assertEquals(1, threads.single { it.id == 1L }.unreadCount)
    }

    @Test fun spamAndArchivedThreadsAreHiddenFromInboxAndQueryableInFolders() {
        dao.insertIncomingSms(message(threadId = 1, at = "2026-01-01T00:00:00Z"))
        dao.insertIncomingSms(message(threadId = 2, at = "2026-01-02T00:00:00Z"))
        dao.insertIncomingSms(message(threadId = 3, at = "2026-01-03T00:00:00Z"))

        dao.updateSpam(2, true)
        dao.updateArchived(3, true)

        assertEquals(listOf(1L), dao.inboxThreads().map { it.id })
        assertEquals(listOf(2L), dao.spamThreads().map { it.id })
        assertEquals(listOf(3L), dao.archivedThreads().map { it.id })
    }

    @Test fun pinnedThreadsSortBeforeRecentUnpinnedThreadsByPinnedDate() {
        dao.insertIncomingSms(message(threadId = 1, at = "2026-01-01T00:00:00Z"))
        dao.insertIncomingSms(message(threadId = 2, at = "2026-01-02T00:00:00Z"))
        dao.insertIncomingSms(message(threadId = 3, at = "2026-01-03T00:00:00Z"))

        dao.updatePinned(1, true, Instant.parse("2026-01-05T00:00:00Z"))
        dao.updatePinned(2, true, Instant.parse("2026-01-04T00:00:00Z"))

        assertEquals(listOf(1L, 2L, 3L), dao.inboxThreads().map { it.id })
    }


    @Test fun batchInsertTracksLatestSyncedTelephonyId() {
        dao.insertSmsBatch(listOf(
            message(threadId = 1, body = "first", at = "2026-01-01T00:00:00Z").copy(telephonyId = 10),
            message(threadId = 1, body = "second", at = "2026-01-02T00:00:00Z").copy(telephonyId = 12),
        ))

        assertEquals(12L, dao.latestSyncedTelephonyId())
        assertEquals(listOf(1L), dao.inboxThreads().map { it.id })
        assertEquals("second", dao.inboxThreads().single().lastMessage)
    }

    @Test fun sendStatusCanBeUpdatedAfterOutgoingInsert() {
        val messageId = dao.insertOutgoingSms(message(threadId = 1, direction = MessageDirection.Outgoing, status = MessageStatus.Sending, read = true))

        dao.updateSendStatus(messageId, MessageStatus.Delivered)

        assertEquals(MessageStatus.Delivered, database.query("SELECT status FROM messages WHERE id = ?", arrayOf(messageId)).use {
            it.moveToFirst()
            MessageStatus.valueOf(it.getString(0))
        })
    }

    private fun message(
        threadId: Long,
        body: String = "message $threadId",
        at: String = "2026-01-01T00:00:00Z",
        direction: MessageDirection = MessageDirection.Incoming,
        status: MessageStatus = MessageStatus.Received,
        read: Boolean = false,
    ) = SmsMessageEntity(
        threadId = threadId,
        address = "+100$threadId",
        body = body,
        timestamp = Instant.parse(at),
        direction = direction,
        status = status,
        read = read,
    )
}
