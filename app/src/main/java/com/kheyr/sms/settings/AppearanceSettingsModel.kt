package com.kheyr.sms.settings

/**
 * Appearance settings model for system, light, and dark theme choices.
 */
data class AppearanceSettingsModel(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
