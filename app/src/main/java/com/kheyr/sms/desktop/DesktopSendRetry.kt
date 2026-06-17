package com.kheyr.sms.desktop

data class DesktopSendRetry(val requestId: String, val attempt: Int, val maxAttempts: Int = 3) {
    val canRetry: Boolean get() = attempt < maxAttempts
    fun next(): DesktopSendRetry = copy(attempt = attempt + 1)
}
