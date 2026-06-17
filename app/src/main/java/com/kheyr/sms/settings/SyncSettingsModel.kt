package com.kheyr.sms.settings

import com.kheyr.sms.sync.SyncStatusModel

data class SyncSettingsModel(val enabled: Boolean, val status: SyncStatusModel, val lastSyncedAtMillis: Long?)
