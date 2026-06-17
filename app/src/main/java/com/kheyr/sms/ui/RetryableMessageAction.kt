package com.kheyr.sms.ui

/**
 * Retryable failed-message action model for conversation rows.
 */
data class RetryableMessageAction(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
