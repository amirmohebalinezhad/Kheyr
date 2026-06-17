package com.kheyr.sms.telephony

/**
 * Dual-SIM selection state model for available SIMs and selected/default SIM.
 */
data class DualSimSelectionState(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
