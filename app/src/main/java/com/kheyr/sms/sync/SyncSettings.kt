package com.kheyr.sms.sync

import java.time.Instant

/**
 * User-controlled sync preferences. Sync is disabled until explicitly enabled.
 */
data class SyncSettings(
    val enabled: Boolean = false,
    val deviceId: String? = null,
    val lastSuccessfulUploadAt: Instant? = null,
) {
    val canUpload: Boolean get() = enabled && !deviceId.isNullOrBlank()
}
