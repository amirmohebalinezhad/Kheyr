package com.kheyr.sms.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SmsThreadEntity::class, SmsMessageEntity::class, ThreadStateEntity::class, SyncSpamMetadataEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsDao(): SmsDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "kheyr_sms.db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
                .also { instance = it }
        }
    }
}
