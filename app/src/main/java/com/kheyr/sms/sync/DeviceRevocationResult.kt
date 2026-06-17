package com.kheyr.sms.sync

data class DeviceRevocationResult(val deviceId: String, val revoked: Boolean, val message: String)
