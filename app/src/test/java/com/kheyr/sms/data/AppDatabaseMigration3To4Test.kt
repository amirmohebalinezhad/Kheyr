package com.kheyr.sms.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The store ships without fallbackToDestructiveMigration, so a wrong or missing MIGRATION_3_4
 * crashes every existing install at launch rather than degrading. This builds a real, populated v3
 * database from the committed 3.json schema and upgrades it: Room re-validates the result against
 * the v4 entities on open, so the test also proves the migrated schema matches 4.json.
 */
@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigration3To4Test {
    private val name = "migration-3-to-4-test.db"
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(name)
    }

    @After fun tearDown() {
        context.deleteDatabase(name)
    }

    @Test fun migratesAPopulatedV3StoreToV4() {
        createPopulatedV3Store()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        try {
            val dao = database.smsDao()

            // Opening ran the migration and Room's schema validation against the v4 entities.
            val messages = dao.messagesForThread(7)
            assertEquals("both pre-existing messages must survive", 2, messages.size)
            assertEquals(listOf("receipt", "promo"), messages.map { it.body }.sorted().reversed())

            messages.forEach { message ->
                assertNull("an existing message has no category yet", message.categoryId)
                assertEquals("and scores zero until it is reclassified", 0, message.spamScore)
            }

            // Thread state written before the upgrade is untouched.
            assertEquals(true, dao.isThreadSpam(9))
            assertEquals(true, dao.isUserNotSpam(9))

            // The point of the migration: a new message carries both axes.
            dao.insertMessage(
                SmsMessageEntity(
                    telephonyId = 300,
                    threadId = 7,
                    address = "20001",
                    body = "تور مشهد example.ir لغو11",
                    timestamp = Instant.ofEpochMilli(5_000),
                    direction = MessageDirection.Incoming,
                    status = MessageStatus.Received,
                    read = false,
                    categoryId = "advertising",
                    spamScore = 40,
                ),
            )
            val stored = dao.messagesForThread(7).single { it.telephonyId == 300L }
            assertEquals("advertising", stored.categoryId)
            assertEquals(40, stored.spamScore)
        } finally {
            database.close()
        }
    }

    @Test fun theMigrationCreatesTheCategoryTables() {
        createPopulatedV3Store()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        try {
            val db = database.openHelper.writableDatabase
            db.execSQL("INSERT INTO categories VALUES ('financial', 'بانکی و مالی', 1, 'blue', 0, 1000, NULL)")
            // senderKey is a PhoneNumberNormalizer.matchKey, not a raw address.
            db.execSQL("INSERT INTO sender_category VALUES ('989120000000', 'financial', 'user', 2000)")

            db.query("SELECT name FROM categories WHERE id = 'financial'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("بانکی و مالی", cursor.getString(0))
            }
            db.query("SELECT categoryId, source FROM sender_category WHERE senderKey = '989120000000'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("financial", cursor.getString(0))
                assertEquals("user", cursor.getString(1))
            }
        } finally {
            database.close()
        }
    }

    /** Exactly the schema 3.json describes, with rows in it, plus Room's identity hash for v3. */
    private fun createPopulatedV3Store() {
        val path = context.getDatabasePath(name).also { it.parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            listOf(
                "CREATE TABLE IF NOT EXISTS `threads` (`id` INTEGER NOT NULL, `address` TEXT NOT NULL, `displayName` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                "CREATE TABLE IF NOT EXISTS `messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `telephonyId` INTEGER, `threadId` INTEGER NOT NULL, `address` TEXT NOT NULL, `body` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `direction` TEXT NOT NULL, `status` TEXT NOT NULL, `read` INTEGER NOT NULL, `simSlot` INTEGER)",
                "CREATE INDEX IF NOT EXISTS `index_messages_threadId` ON `messages` (`threadId`)",
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_messages_telephonyId` ON `messages` (`telephonyId`)",
                "CREATE TABLE IF NOT EXISTS `thread_state` (`threadId` INTEGER NOT NULL, `isPinned` INTEGER NOT NULL, `pinnedAt` INTEGER, `isMuted` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `isSpam` INTEGER NOT NULL, `userNotSpam` INTEGER NOT NULL, PRIMARY KEY(`threadId`))",
                "CREATE TABLE IF NOT EXISTS `sync_spam_metadata` (`threadId` INTEGER NOT NULL, `lastSyncedTelephonyMessageId` INTEGER, `lastSyncedAt` INTEGER, `spamScore` REAL NOT NULL, `spamReason` TEXT, `markedSpamAt` INTEGER, PRIMARY KEY(`threadId`))",
                "CREATE TABLE IF NOT EXISTS `sync_queue` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `eventType` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `uploaded` INTEGER NOT NULL)",
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
                "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f4601763f1ed17b96444b7cc3c0e75f5')",
            ).forEach(db::execSQL)
            db.execSQL("INSERT INTO threads VALUES (7, '20001', '20001', 1000)")
            db.execSQL("INSERT INTO threads VALUES (9, '+989120000000', 'Ali', 1000)")
            db.execSQL("INSERT INTO messages VALUES (1, 100, 7, '20001', 'receipt', 2000, 'Incoming', 'Received', 1, NULL)")
            db.execSQL("INSERT INTO messages VALUES (2, 200, 7, '20001', 'promo', 3000, 'Incoming', 'Received', 0, NULL)")
            // A thread the user has already restored with "Not spam" - the correction must survive.
            db.execSQL("INSERT INTO thread_state VALUES (9, 0, NULL, 0, 0, 1, 1)")
            db.execSQL("PRAGMA user_version = 3")
        }
    }
}
