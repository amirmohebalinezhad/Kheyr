package com.kheyr.sms.ui

/**
 * Conversation UI state model for header, messages, composer, and search.
 */
data class ConversationUiState(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
