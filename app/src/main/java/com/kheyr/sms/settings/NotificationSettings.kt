package com.kheyr.sms.settings

data class NotificationSettings(
    val contentMode: NotificationContentMode = NotificationContentMode.ShowSenderAndPreview,
    val unknownSenderMode: UnknownSenderNotificationMode = UnknownSenderNotificationMode.Normal,
    val vibrate: Boolean = true,
    val globalRingtoneUri: String? = null,
)

enum class NotificationContentMode {
    ShowSenderAndPreview,
    ShowSenderOnly,
    HideSenderAndPreview,
}

enum class UnknownSenderNotificationMode {
    Normal,
    Silent,
    None,
}

data class ThreadNotificationSettings(
    val threadId: Long,
    val muted: Boolean = false,
    val customRingtoneUri: String? = null,
)
