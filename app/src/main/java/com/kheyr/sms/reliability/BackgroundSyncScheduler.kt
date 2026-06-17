package com.kheyr.sms.reliability

object BackgroundSyncScheduler {
    const val PERIODIC_INTERVAL_MINUTES: Long = 15
    fun shouldSchedule(syncEnabled: Boolean): Boolean = syncEnabled
}
