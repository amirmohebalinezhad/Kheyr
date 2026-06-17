package com.kheyr.sms.data

/**
 * Thread action model for pin, archive, delete, spam, read, mute, and notification settings actions.
 */
data class ThreadActionModel(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
