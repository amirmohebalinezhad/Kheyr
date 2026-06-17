package com.kheyr.sms.data

/**
 * Unread thread state model for unread count and read sync timestamp.
 */
data class UnreadThreadState(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
