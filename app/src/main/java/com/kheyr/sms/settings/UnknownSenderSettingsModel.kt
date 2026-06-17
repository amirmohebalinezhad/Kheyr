package com.kheyr.sms.settings

/**
 * Unknown sender settings model for normal, silent, and suppressed notifications.
 */
data class UnknownSenderSettingsModel(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
