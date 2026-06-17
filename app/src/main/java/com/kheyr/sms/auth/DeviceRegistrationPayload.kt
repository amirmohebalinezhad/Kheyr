package com.kheyr.sms.auth

data class DeviceRegistrationPayload(
    val deviceName: String,
    val deviceType: String = "android",
    val platform: String,
    val publicKey: String,
    val pushToken: String? = null,
)
