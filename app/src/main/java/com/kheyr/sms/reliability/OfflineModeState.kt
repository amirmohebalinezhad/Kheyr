package com.kheyr.sms.reliability

data class OfflineModeState(val networkAvailable: Boolean, val queuedOperations: Int = 0) {
    val isOffline: Boolean get() = !networkAvailable
}
