package com.kheyr.sms

import android.app.Application
import android.util.Log
import com.kheyr.sms.data.AppDatabase
import com.kheyr.sms.preferences.AppPreferences
import com.kheyr.sms.worker.KheyrWorkerScheduler

class KheyrApplication : Application() {
    lateinit var preferences: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(this)
        KheyrWorkerScheduler.scheduleAll(this, preferences.syncSettings().enabled)
        // Off the main thread: opening the SQLCipher store runs PBKDF2 key derivation. Doing it here
        // lets the app heal an unreadable encrypted database at startup instead of crashing on the
        // first query of every launch, incoming SMS and notification action (B-05).
        Thread {
            try {
                AppDatabase.ensureOpenOrRecreate(this@KheyrApplication)
            } catch (t: Throwable) {
                Log.e("KheyrApplication", "Failed to verify the encrypted database", t)
            }
        }.start()
    }
}
