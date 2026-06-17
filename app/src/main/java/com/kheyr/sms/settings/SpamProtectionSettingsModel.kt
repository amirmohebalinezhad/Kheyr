package com.kheyr.sms.settings

/**
 * Spam protection settings model exposing enabled state, rule version, and threshold.
 */
data class SpamProtectionSettingsModel(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
