package com.kheyr.sms.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A real v2 store, created from the committed 2.json schema, must survive MIGRATION_2_3 with its
 * rows intact and pick up userNotSpam = 0. Room re-validates the schema on open, so this also
 * proves the migrated table matches what the v3 entity expects.
 */
@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {
    private val name = "migration-test.db"
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(name)
    }

    @After fun tearDown() {
        context.deleteDatabase(name)
    }

    @Test fun migratesV2StoreToV3PreservingRows() {
        val path = context.getDatabasePath(name).also { it.parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            listOf(
            "CREATE TABLE IF NOT EXISTS `threads` (`id` INTEGER NOT NULL, `address` TEXT NOT NULL, `displayName` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `telephonyId` INTEGER, `threadId` INTEGER NOT NULL, `address` TEXT NOT NULL, `body` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `direction` TEXT NOT NULL, `status` TEXT NOT NULL, `read` INTEGER NOT NULL, `simSlot` INTEGER)",
            "CREATE INDEX IF NOT EXISTS `index_messages_threadId` ON `messages` (`threadId`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_messages_telephonyId` ON `messages` (`telephonyId`)",
            "CREATE TABLE IF NOT EXISTS `thread_state` (`threadId` INTEGER NOT NULL, `isPinned` INTEGER NOT NULL, `pinnedAt` INTEGER, `isMuted` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `isSpam` INTEGER NOT NULL, PRIMARY KEY(`threadId`))",
            "CREATE TABLE IF NOT EXISTS `sync_spam_metadata` (`threadId` INTEGER NOT NULL, `lastSyncedTelephonyMessageId` INTEGER, `lastSyncedAt` INTEGER, `spamScore` REAL NOT NULL, `spamReason` TEXT, `markedSpamAt` INTEGER, PRIMARY KEY(`threadId`))",
            "CREATE TABLE IF NOT EXISTS `sync_queue` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `eventType` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `uploaded` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '6cdcbb53c5080d015cfb96ac328d80e1')",
            ).forEach(db::execSQL)
            db.execSQL("INSERT INTO threads VALUES (7, '+15551234567', '+15551234567', 1000)")
            db.execSQL("INSERT INTO thread_state VALUES (7, 1, 2000, 0, 0, 1)")
            db.execSQL("PRAGMA user_version = 2")
        }

        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        try {
            // Opening runs the migration and Room's own schema validation against the v3 entities.
            val thread = database.smsDao().inboxThreads()
            assertTrue("pre-existing spam thread should still be flagged spam", database.smsDao().isThreadSpam(7) == true)
            assertEquals("the user has not corrected this thread", false, database.smsDao().isUserNotSpam(7))
            assertEquals("spam threads are hidden from the inbox", 0, thread.size)

            // The whole point of the column: an automatic re-flag must not undo a user correction.
            database.smsDao().updateSpam(7, false)
            assertEquals(true, database.smsDao().isUserNotSpam(7))
            database.smsDao().autoMarkSpam(7)
            assertEquals("autoMarkSpam must respect the correction", false, database.smsDao().isThreadSpam(7))
        } finally {
            database.close()
        }
    }
}
