package com.kheyr.sms.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [SmsThreadEntity::class, SmsMessageEntity::class, ThreadStateEntity::class, SyncSpamMetadataEntity::class, SyncQueueEntity::class],
    version = 2,
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
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
