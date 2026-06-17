package com.kheyr.sms.sync

/**
 * Desktop SMS relay status model including waiting-for-phone and retryable failure.
 */
data class DesktopRelayStatus(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
