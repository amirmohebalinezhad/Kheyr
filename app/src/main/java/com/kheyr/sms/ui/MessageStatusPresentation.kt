package com.kheyr.sms.ui

/**
 * Message status presentation labels for sending, sent, delivered, and failed states.
 */
data class MessageStatusPresentation(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
