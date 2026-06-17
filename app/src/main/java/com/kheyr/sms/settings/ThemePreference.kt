package com.kheyr.sms.settings

enum class ThemePreference { System, Light, Dark }
object ThemePreferenceResolver {
    fun isDark(preference: ThemePreference, systemDark: Boolean): Boolean = when (preference) {
        ThemePreference.System -> systemDark
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }
}
