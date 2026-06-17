package com.kheyr.sms.ui

/**
 * Conversation search state model with query, matches, and selected match.
 */
data class ConversationSearchState(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
