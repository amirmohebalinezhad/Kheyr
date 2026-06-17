package com.kheyr.sms.sync

data class InitialSyncProgress(val uploaded: Int, val total: Int) {
    val complete: Boolean get() = total >= 0 && uploaded >= total
    val percent: Int get() = if (total <= 0) 0 else uploaded.coerceAtMost(total) * 100 / total
}
