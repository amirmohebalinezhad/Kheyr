package com.kheyr.sms.privacy

data class CloudDataDeletionRequest(val userId: String, val deleteSyncedMessages: Boolean, val revokeDevices: Boolean) {
    val isDestructive: Boolean get() = deleteSyncedMessages || revokeDevices
}
