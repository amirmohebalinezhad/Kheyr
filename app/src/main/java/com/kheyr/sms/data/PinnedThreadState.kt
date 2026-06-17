package com.kheyr.sms.data

/**
 * Pinned thread state model with pin timestamp ordering support.
 */
data class PinnedThreadState(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
