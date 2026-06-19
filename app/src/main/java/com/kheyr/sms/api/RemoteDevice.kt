package com.kheyr.sms.api

/** A device registered to the signed-in account, as returned by GET /api/v1/devices. */
data class RemoteDevice(
    val id: String,
    val name: String,
    val type: String,        // "android" | "desktop"
    val platform: String,    // "Windows" | "MacOS" | "Linux" | "android"
    val lastActiveAtEpochSeconds: Long?,
) {
    val isDesktop: Boolean get() = type.equals("desktop", ignoreCase = true)
}
