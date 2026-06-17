package com.kheyr.sms.domain

/**
 * User spam correction model for Mark Spam and Mark Not Spam actions.
 */
data class UserSpamCorrection(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
