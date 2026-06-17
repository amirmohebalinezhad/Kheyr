package com.kheyr.sms.sync

data class SyncUpdateApplierResult(val applied: Int, val skipped: Int)

class SyncUpdateApplier {
    fun apply(updates: List<EncryptedSyncRecord>): SyncUpdateApplierResult {
        val applied = updates.count { it.isVisible }
        return SyncUpdateApplierResult(applied = applied, skipped = updates.size - applied)
    }
}
