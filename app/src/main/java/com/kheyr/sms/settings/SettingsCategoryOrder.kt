package com.kheyr.sms.settings

enum class SettingsCategory { Notifications, UnknownSenders, SpamProtection, DualSim, Sync, DesktopDevices, PrivacySecurity, Appearance, About }
object SettingsCategoryOrder {
    val ordered: List<SettingsCategory> = listOf(SettingsCategory.Notifications, SettingsCategory.UnknownSenders, SettingsCategory.SpamProtection, SettingsCategory.DualSim, SettingsCategory.Sync, SettingsCategory.DesktopDevices, SettingsCategory.PrivacySecurity, SettingsCategory.Appearance, SettingsCategory.About)
}
