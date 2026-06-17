package com.kheyr.sms.data

/**
 * Message action model for copy, delete, spam correction, and retry actions.
 */
data class MessageActionModel(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
