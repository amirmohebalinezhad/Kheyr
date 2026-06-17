package com.kheyr.sms.ui

/**
 * Conversation message UI model for bubble, timestamp, and status rendering.
 */
data class ConversationMessageUiModel(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
