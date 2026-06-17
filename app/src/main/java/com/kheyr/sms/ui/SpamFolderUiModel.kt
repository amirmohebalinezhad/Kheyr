package com.kheyr.sms.ui

/**
 * Spam folder UI model with unread count and empty-state text.
 */
data class SpamFolderUiModel(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
