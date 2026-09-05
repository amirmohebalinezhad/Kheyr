package com.kheyr.sms.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: buildEncryptedDatabase(context.applicationContext).also { instance = it }
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
            val database = getInstance(appContext)
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
    }
}
