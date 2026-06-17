package com.kheyr.sms.sync

/**
 * Desktop device list model for paired-device management and revocation UI.
 */
data class DesktopDeviceListModel(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
