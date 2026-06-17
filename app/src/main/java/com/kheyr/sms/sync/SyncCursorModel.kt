package com.kheyr.sms.sync

/**
 * Sync cursor model for initial and incremental sync checkpoints.
 */
data class SyncCursorModel(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
