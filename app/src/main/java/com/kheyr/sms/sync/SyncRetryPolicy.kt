package com.kheyr.sms.sync

object SyncRetryPolicy {
    fun delayMillis(attempt: Int, baseMillis: Long = 1_000L, maxMillis: Long = 60_000L): Long =
        minOf(baseMillis * (1L shl attempt.coerceAtMost(6)), maxMillis)
}
