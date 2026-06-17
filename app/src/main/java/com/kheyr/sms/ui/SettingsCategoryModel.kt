package com.kheyr.sms.ui

/**
 * Settings category model in the PRD-required order.
 */
data class SettingsCategoryModel(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
