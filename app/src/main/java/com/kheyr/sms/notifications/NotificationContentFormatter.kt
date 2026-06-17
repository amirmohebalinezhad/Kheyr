package com.kheyr.sms.notifications

enum class NotificationPrivacyMode { Preview, SenderOnly, Hidden }
data class NotificationContent(val title: String, val text: String)
object NotificationContentFormatter {
    fun format(sender: String, body: String, mode: NotificationPrivacyMode): NotificationContent = when (mode) {
        NotificationPrivacyMode.Preview -> NotificationContent(sender, body)
        NotificationPrivacyMode.SenderOnly -> NotificationContent(sender, "New message")
        NotificationPrivacyMode.Hidden -> NotificationContent("New SMS", "Content hidden")
    }
}
