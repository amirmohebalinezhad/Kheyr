package com.kheyr.sms.data

/**
 * Archive state model for archive visibility and sync metadata.
 */
data class ArchiveStateModel(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
