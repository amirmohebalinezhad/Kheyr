package com.kheyr.sms.data

import android.content.Context
import android.os.Looper
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [SmsThreadEntity::class, SmsMessageEntity::class, ThreadStateEntity::class, SyncSpamMetadataEntity::class, SyncQueueEntity::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsDao(): SmsDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /**
         * Non-null only while [ensureOpenOrRecreate] is checking - and possibly deleting and
         * rebuilding - the encrypted store. [getInstance] waits on it so no DAO is handed out while
         * the file underneath it is being replaced.
         */
        @Volatile private var recovery: CountDownLatch? = null

        /**
         * Migration from schema v1 to v2. The only delta between v1 and v2 (introduced in commit
         * 4ee5eaf, which bumped `version` from 1 to 2) is the addition of the `sync_queue` table
         * backing [SyncQueueEntity]; no existing table or column was altered. This migration creates
         * that table to match 2.json exactly so that an existing v1 encrypted store is preserved.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sync_queue` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`eventType` TEXT NOT NULL, " +
                        "`payloadJson` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`uploaded` INTEGER NOT NULL)",
                )
            }
        }

        /**
         * Migration from schema v2 to v3. The only delta is the `userNotSpam` column added to
         * `thread_state` so an explicit "Not spam" correction survives the automatic classifier
         * (B-23); no other table or column changed, so a plain ADD COLUMN with the entity's default
         * preserves an existing v2 encrypted store.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE thread_state ADD COLUMN userNotSpam INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            val database = instance ?: synchronized(this) {
                instance ?: buildEncryptedDatabase(context.applicationContext).also { instance = it }
            }
            awaitRecovery()
            return database
        }

        /**
         * Blocks until the startup check in [ensureOpenOrRecreate] has finished.
         *
         * That check runs on its own thread, and when the store turns out to be undecryptable it
         * deletes the database file and reopens it. A query issued in that window - the receiver
         * persisting an incoming SMS, a worker draining the sync queue - would run against a file
         * that is being deleted underneath it, which is the crash B-05 exists to heal rather than
         * cause. Waiting here holds those callers until the store is known good.
         *
         * Only ever waits during a recovery: [recovery] is null on every normal launch (and in the
         * JVM unit tests, which is why the null check comes before [Looper] is touched). The main
         * thread is never held: Room refuses to run a query there anyway, so a main-thread caller is
         * only obtaining the handle, and stalling it on SQLCipher's key derivation would risk an ANR.
         */
        private fun awaitRecovery() {
            val latch = recovery ?: return
            if (Looper.myLooper() == Looper.getMainLooper()) return
            if (!latch.await(RECOVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                Log.w(TAG, "Timed out waiting for the encrypted database check to finish")
            }
        }

        private fun buildEncryptedDatabase(context: Context): AppDatabase {
            val passphrase = LocalDatabasePassphraseStore(context).getOrCreatePassphrase()
            val factory = SupportFactory(passphrase)
            // No destructive fallback: a missing migration must fail loudly rather than silently
            // wiping the user's encrypted SMS store.
            return Room.databaseBuilder(context, AppDatabase::class.java, EncryptedDatabasePolicy.databaseFileName)
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }

        /**
         * Opens the encrypted store once at startup and, if it cannot be opened, deletes it and
         * rebuilds it from scratch.
         *
         * The store is unopenable when the SQLCipher passphrase no longer matches the database file:
         * a cloud-backup restore whose Keystore master key did not come along, or an invalidated
         * master key, after which [LocalDatabasePassphraseStore] hands out a brand-new random
         * passphrase. SQLCipher then throws on the very first query, so every launch, every incoming
         * SMS and every notification action crashes forever (B-05). The existing rows are encrypted
         * with a key nobody holds any more, so they are already lost; deleting the files is the only
         * way to get a working app back.
         *
         * Must be called off the main thread: forcing the open runs SQLCipher's PBKDF2 key
         * derivation, which is far too slow for the UI thread. [getInstance] therefore stays lazy.
         */
        fun ensureOpenOrRecreate(context: Context) {
            val appContext = context.applicationContext
            // Fetch the handle BEFORE arming the latch, otherwise this thread would wait on itself.
            val database = getInstance(appContext)
            val latch = CountDownLatch(1)
            recovery = latch
            try {
                probeAndRecover(appContext, database)
            } finally {
                recovery = null
                latch.countDown()
            }
        }

        private fun probeAndRecover(appContext: Context, database: AppDatabase) {
            try {
                // Touching the readable database forces the open (and the key derivation) to happen here.
                database.openHelper.readableDatabase
                return
            } catch (t: Throwable) {
                if (!isUndecryptableStore(t)) {
                    // Anything else — a missing migration, a failed identity-hash check, a full disk —
                    // must still fail loudly. Deleting here would turn "we forgot a migration" into
                    // "every user silently lost their SMS", which is exactly what buildEncryptedDatabase
                    // refuses to do by leaving out fallbackToDestructiveMigration().
                    Log.e(TAG, "Encrypted database could not be opened; NOT recreating it", t)
                    throw t
                }
                Log.e(
                    TAG,
                    "Encrypted database cannot be decrypted with the current passphrase; " +
                        "deleting the unreadable store and recreating it",
                    t,
                )
            }
            synchronized(this) {
                // Keep the SAME AppDatabase instance rather than swapping `instance`. SmsRepository
                // captures its DAO at construction, and the main thread can already hold one by the
                // time this startup check runs; replacing the instance would leave those DAOs bound
                // to a closed database and crash the very launch this is meant to heal. Closing just
                // the open helper lets the same instance open a fresh file underneath.
                runCatching { database.openHelper.close() }
                appContext.deleteDatabase(EncryptedDatabasePolicy.databaseFileName)
                database.openHelper.readableDatabase
            }
        }

        /**
         * True only when the failure means "this file cannot be decrypted with the passphrase we
         * hold" — SQLCipher reports that as a SQLite exception complaining the file is encrypted or
         * is not a database, and a corrupt-database exception is equally unrecoverable. Matching on
         * class name rather than type keeps the SQLCipher classes off the JVM unit-test class path.
         */
        internal fun isUndecryptableStore(error: Throwable): Boolean =
            generateSequence(error, Throwable::cause).any { cause ->
                val name = cause.javaClass.name
                when {
                    name.endsWith("SQLiteDatabaseCorruptException") -> true
                    !name.endsWith("SQLiteException") -> false
                    else -> cause.message.orEmpty().let {
                        it.contains("not a database", ignoreCase = true) ||
                            it.contains("file is encrypted", ignoreCase = true)
                    }
                }
            }

        private const val TAG = "AppDatabase"

        /**
         * Upper bound on how long a caller waits for the startup check. Long enough to cover key
         * derivation plus deleting and rebuilding the store on a slow device, short enough that a
         * wedged check degrades to the old behaviour instead of hanging a worker for good.
         */
        private const val RECOVERY_TIMEOUT_SECONDS = 20L
    }
}
