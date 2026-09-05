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
        //
        // onCreate cannot block on this - key derivation and a rebuild are far too slow to hold up
        // process start - so the check runs detached and AppDatabase.getInstance() holds any other
        // thread that asks for a DAO until it has finished. That is what keeps a receiver or worker
        // from querying the store in the window where it is being deleted and recreated.
        Thread {
            try {
                AppDatabase.ensureOpenOrRecreate(this@KheyrApplication)
            } catch (t: Throwable) {
                Log.e("KheyrApplication", "Failed to verify the encrypted database", t)
            }
        }.start()
    }
}
