package com.kheyr.sms.desktop

object DesktopRelayQueueExpiry {
    fun isExpired(createdAtMillis: Long, nowMillis: Long = System.currentTimeMillis(), ttlMillis: Long = 86_400_000L): Boolean =
        nowMillis - createdAtMillis >= ttlMillis
}
