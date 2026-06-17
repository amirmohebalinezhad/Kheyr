package com.kheyr.sms

import android.app.Application
import com.kheyr.sms.preferences.AppPreferences
import com.kheyr.sms.worker.KheyrWorkerScheduler

class KheyrApplication : Application() {
    lateinit var preferences: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(this)
        KheyrWorkerScheduler.scheduleAll(this, preferences.syncSettings().enabled)
    }
}
