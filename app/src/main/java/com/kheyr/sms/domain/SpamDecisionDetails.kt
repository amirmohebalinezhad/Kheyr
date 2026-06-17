package com.kheyr.sms.domain

/**
 * Spam decision details model with score, classification, rule version, and reasons.
 */
data class SpamDecisionDetails(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
