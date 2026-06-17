package com.kheyr.sms.sync

data class SyncEnablementGate(val userOptedIn: Boolean, val accountSignedIn: Boolean) {
    val canUpload: Boolean get() = userOptedIn && accountSignedIn
}
