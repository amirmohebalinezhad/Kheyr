package com.kheyr.sms.ui

/**
 * Phase 2 direct message availability model with SMS fallback visibility.
 */
data class DirectMessageAvailabilityModel(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
