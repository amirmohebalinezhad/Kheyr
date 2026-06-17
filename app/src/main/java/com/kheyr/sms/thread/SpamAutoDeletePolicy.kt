package com.kheyr.sms.thread

object SpamAutoDeletePolicy {
    fun shouldDelete(spamAtMillis: Long, retentionDays: Int, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (retentionDays <= 0) return false
        return nowMillis - spamAtMillis >= retentionDays * 86_400_000L
    }
}
