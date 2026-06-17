package com.kheyr.sms.telephony

/**
 * Clear SMS send failure reason model for user-facing errors.
 */
data class SmsSendFailureReason(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
