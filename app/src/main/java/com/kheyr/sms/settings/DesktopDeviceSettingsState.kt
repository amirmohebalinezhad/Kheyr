package com.kheyr.sms.settings

data class DesktopDeviceSettingsState(val pairedDeviceCount: Int, val revokedDeviceCount: Int) {
    val hasPairedDevices: Boolean get() = pairedDeviceCount > 0
}
