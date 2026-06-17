package com.kheyr.sms.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: buildEncryptedDatabase(context.applicationContext).also { instance = it }
        }

        private fun buildEncryptedDatabase(context: Context): AppDatabase {
            val passphrase = LocalDatabasePassphraseStore(context).getOrCreatePassphrase()
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(context, AppDatabase::class.java, EncryptedDatabasePolicy.databaseFileName)
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
