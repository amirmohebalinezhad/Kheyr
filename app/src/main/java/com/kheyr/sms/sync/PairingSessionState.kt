package com.kheyr.sms.sync

/**
 * QR pairing session state model with expiry and approval states.
 */
data class PairingSessionState(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
