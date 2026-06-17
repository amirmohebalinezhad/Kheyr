package com.kheyr.sms.domain

/**
 * Spam rule cache state model for latest valid cached rule fallback.
 */
data class SpamRuleCacheState(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
