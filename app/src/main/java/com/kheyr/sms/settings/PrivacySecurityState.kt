package com.kheyr.sms.settings

/**
 * Privacy and security settings state for notification privacy and local encryption availability.
 */
data class PrivacySecurityState(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
