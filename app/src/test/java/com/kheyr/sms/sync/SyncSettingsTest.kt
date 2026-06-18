package com.kheyr.sms.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSettingsTest {
    @Test fun canUploadRequiresEnabledAndPairedDevice() {
        assertTrue(SyncSettings(enabled = true, deviceId = "device-1").canUpload)
    }

    @Test fun cannotUploadWhenDisabled() {
        assertFalse(SyncSettings(enabled = false, deviceId = "device-1").canUpload)
    }

    @Test fun cannotUploadWhenDeviceUnpaired() {
        assertFalse(SyncSettings(enabled = true, deviceId = null).canUpload)
        assertFalse(SyncSettings(enabled = true, deviceId = "").canUpload)
        assertFalse(SyncSettings(enabled = true, deviceId = "   ").canUpload)
    }
}
