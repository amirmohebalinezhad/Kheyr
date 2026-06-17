package com.kheyr.sms.sync

/**
 * Encrypted sync record model that keeps SMS body ciphertext separate from minimized metadata.
 */
data class EncryptedSyncRecord(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
