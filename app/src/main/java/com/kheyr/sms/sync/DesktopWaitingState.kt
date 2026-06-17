package com.kheyr.sms.sync

data class DesktopWaitingState(val requestId: String, val phoneLastSeenAtMillis: Long?, val queuedAtMillis: Long) {
    fun message(nowMillis: Long): String = if (phoneLastSeenAtMillis == null) "Waiting for phone" else "Waiting for phone, last seen ${nowMillis - phoneLastSeenAtMillis} ms ago"
}
