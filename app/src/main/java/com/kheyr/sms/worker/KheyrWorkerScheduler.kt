package com.kheyr.sms.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kheyr.sms.reliability.BackgroundSyncScheduler
import java.util.concurrent.TimeUnit

object KheyrWorkerScheduler {
    private const val SYNC_WORK = "kheyr_sync"
    private const val SPAM_RULES_WORK = "kheyr_spam_rules"

    fun scheduleAll(context: Context, syncEnabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (BackgroundSyncScheduler.shouldSchedule(syncEnabled)) {
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(BackgroundSyncScheduler.PERIODIC_INTERVAL_MINUTES, TimeUnit.MINUTES).build()
            workManager.enqueueUniquePeriodicWork(SYNC_WORK, ExistingPeriodicWorkPolicy.UPDATE, syncRequest)
        } else {
            workManager.cancelUniqueWork(SYNC_WORK)
        }
        val spamRequest = PeriodicWorkRequestBuilder<SpamRulesWorker>(6, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(SPAM_RULES_WORK, ExistingPeriodicWorkPolicy.UPDATE, spamRequest)
    }
}
