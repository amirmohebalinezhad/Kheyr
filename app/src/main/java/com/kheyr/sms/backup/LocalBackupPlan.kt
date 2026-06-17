package com.kheyr.sms.backup

data class LocalBackupPlan(val includeMessages: Boolean, val includeSettings: Boolean, val encrypted: Boolean) {
    val isSafeToShare: Boolean get() = encrypted
}
