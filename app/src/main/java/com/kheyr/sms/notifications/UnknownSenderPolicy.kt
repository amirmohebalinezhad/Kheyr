package com.kheyr.sms.notifications

enum class UnknownSenderMode { Normal, Silent, Suppressed }
data class UnknownSenderNotificationDecision(val showNotification: Boolean, val silent: Boolean)
object UnknownSenderPolicy {
    fun decide(mode: UnknownSenderMode): UnknownSenderNotificationDecision = when (mode) {
        UnknownSenderMode.Normal -> UnknownSenderNotificationDecision(true, false)
        UnknownSenderMode.Silent -> UnknownSenderNotificationDecision(true, true)
        UnknownSenderMode.Suppressed -> UnknownSenderNotificationDecision(false, true)
    }
}
