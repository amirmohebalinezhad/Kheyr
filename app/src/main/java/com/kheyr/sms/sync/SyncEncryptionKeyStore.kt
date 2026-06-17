package com.kheyr.sms.sync

data class SyncEncryptionKeyStoreState(val hasKey: Boolean, val alias: String = "kheyr_sync_key") {
    val readyForUpload: Boolean get() = hasKey
}
