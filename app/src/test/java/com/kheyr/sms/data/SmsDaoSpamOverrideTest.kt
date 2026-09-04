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

/** Covers the "Not spam" override (B-23) and the thread row preserved on re-sync (B-30). */
@RunWith(RobolectricTestRunner::class)
class SmsDaoSpamOverrideTest {
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

    @Test fun autoMarkSpamIsIgnoredAfterTheUserMarkedTheThreadNotSpam() {
        dao.insertIncomingSms(message(threadId = 1))
        dao.updateSpam(1, true)
        dao.updateSpam(1, false)

        dao.autoMarkSpam(1)

        assertEquals(false, dao.isThreadSpam(1))
        assertEquals(true, dao.isUserNotSpam(1))
        assertEquals(listOf(1L), dao.inboxThreads().map { it.id })
    }

    @Test fun autoMarkSpamFlagsAThreadTheUserNeverCorrected() {
        dao.insertIncomingSms(message(threadId = 2))

        dao.autoMarkSpam(2)

        assertEquals(true, dao.isThreadSpam(2))
        // The automatic path must not claim the user made a decision.
        assertEquals(false, dao.isUserNotSpam(2))
        assertEquals(listOf(2L), dao.spamThreads().map { it.id })
    }

    @Test fun markingSpamAgainClearsTheNotSpamOverride() {
        dao.insertIncomingSms(message(threadId = 3))
        dao.updateSpam(3, false)
        dao.updateSpam(3, true)
        dao.updateSpam(3, false)
        dao.updateSpam(3, true)

        assertEquals(false, dao.isUserNotSpam(3))
        assertEquals(true, dao.isThreadSpam(3))
    }

    @Test fun telephonyUpsertOfAnExistingMessageKeepsTheThreadDisplayName() {
        dao.upsertTelephonyMessage(message(threadId = 1, at = "2026-01-01T00:00:00Z").copy(telephonyId = 42))
        dao.upsertThread(
            SmsThreadEntity(
                id = 1,
                address = "+1001",
                displayName = "Alice",
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        dao.upsertTelephonyMessage(
            message(threadId = 1, body = "resynced", at = "2026-01-02T00:00:00Z").copy(telephonyId = 42),
        )

        assertEquals("Alice", dao.inboxThreads().single().displayName)
    }

    private fun message(
        threadId: Long,
        body: String = "message $threadId",
        at: String = "2026-01-01T00:00:00Z",
    ) = SmsMessageEntity(
        threadId = threadId,
        address = "+100$threadId",
        body = body,
        timestamp = Instant.parse(at),
        direction = MessageDirection.Incoming,
        status = MessageStatus.Received,
        read = false,
    )
}
