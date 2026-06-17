package com.kheyr.sms.ui

/**
 * Desktop conversation mock model for validating Avalonia UI direction against Android state.
 */
data class DesktopMockConversationModel(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
