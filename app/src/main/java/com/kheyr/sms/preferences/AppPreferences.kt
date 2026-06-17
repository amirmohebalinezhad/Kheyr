package com.kheyr.sms.preferences

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.kheyr.sms.domain.SpamRule
import com.kheyr.sms.domain.SpamRuleSet
import com.kheyr.sms.domain.SpamRuleType
import com.kheyr.sms.settings.NotificationContentMode
import com.kheyr.sms.settings.NotificationSettings
import com.kheyr.sms.settings.ThemePreference
import com.kheyr.sms.settings.UnknownSenderNotificationMode
import com.kheyr.sms.sync.SyncSettings
import java.time.Instant

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()

    var syncOptInSkipped: Boolean
        get() = prefs.getBoolean(KEY_SYNC_OPT_IN_SKIPPED, false)
        set(value) = prefs.edit().putBoolean(KEY_SYNC_OPT_IN_SKIPPED, value).apply()

    var themePreference: ThemePreference
        get() = runCatching { ThemePreference.valueOf(prefs.getString(KEY_THEME, ThemePreference.System.name)!!) }.getOrDefault(ThemePreference.System)
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    var defaultSubscriptionId: Int?
        get() = prefs.getInt(KEY_DEFAULT_SUB, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_DEFAULT_SUB) else putInt(KEY_DEFAULT_SUB, value)
        }.apply()

    var directMessagesEnabled: Boolean
        get() = prefs.getBoolean(KEY_DIRECT_MESSAGES, true)
        set(value) = prefs.edit().putBoolean(KEY_DIRECT_MESSAGES, value).apply()

    var spamAutoDeleteDays: Int
        get() = prefs.getInt(KEY_SPAM_AUTO_DELETE_DAYS, 0)
        set(value) = prefs.edit().putInt(KEY_SPAM_AUTO_DELETE_DAYS, value).apply()

    fun notificationSettings(): NotificationSettings = NotificationSettings(
        contentMode = runCatching { NotificationContentMode.valueOf(prefs.getString(KEY_NOTIF_CONTENT, NotificationContentMode.ShowSenderAndPreview.name)!!) }
            .getOrDefault(NotificationContentMode.ShowSenderAndPreview),
        unknownSenderMode = runCatching { UnknownSenderNotificationMode.valueOf(prefs.getString(KEY_UNKNOWN_SENDER, UnknownSenderNotificationMode.Normal.name)!!) }
            .getOrDefault(UnknownSenderNotificationMode.Normal),
        vibrate = prefs.getBoolean(KEY_VIBRATE, true),
        globalRingtoneUri = prefs.getString(KEY_GLOBAL_RINGTONE, null),
    )

    fun saveNotificationSettings(settings: NotificationSettings) {
        prefs.edit()
            .putString(KEY_NOTIF_CONTENT, settings.contentMode.name)
            .putString(KEY_UNKNOWN_SENDER, settings.unknownSenderMode.name)
            .putBoolean(KEY_VIBRATE, settings.vibrate)
            .apply {
                if (settings.globalRingtoneUri == null) remove(KEY_GLOBAL_RINGTONE) else putString(KEY_GLOBAL_RINGTONE, settings.globalRingtoneUri)
            }
            .apply()
    }

    fun syncSettings(): SyncSettings = SyncSettings(
        enabled = prefs.getBoolean(KEY_SYNC_ENABLED, false),
        deviceId = prefs.getString(KEY_DEVICE_ID, null),
        lastSuccessfulUploadAt = prefs.getString(KEY_LAST_SYNC, null)?.let(Instant::parse),
    )

    fun saveSyncSettings(settings: SyncSettings) {
        prefs.edit()
            .putBoolean(KEY_SYNC_ENABLED, settings.enabled)
            .apply {
                if (settings.deviceId == null) remove(KEY_DEVICE_ID) else putString(KEY_DEVICE_ID, settings.deviceId)
                if (settings.lastSuccessfulUploadAt == null) remove(KEY_LAST_SYNC) else putString(KEY_LAST_SYNC, settings.lastSuccessfulUploadAt.toString())
            }
            .apply()
    }

    fun authTokens(): Pair<String?, String?> = prefs.getString(KEY_ACCESS_TOKEN, null) to prefs.getString(KEY_REFRESH_TOKEN, null)

    fun saveAuthTokens(accessToken: String, refreshToken: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken).putString(KEY_REFRESH_TOKEN, refreshToken).apply()
    }

    fun clearAuthTokens() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).apply()
    }

    fun saveSpamRuleSet(ruleSet: SpamRuleSet) {
        val encoded = ruleSet.rules.joinToString("\n") { rule ->
            listOf(Uri.encode(rule.id), rule.type.name, Uri.encode(rule.pattern.orEmpty()), rule.score, rule.enabled).joinToString("|")
        }
        prefs.edit()
            .putInt(KEY_SPAM_VERSION, ruleSet.version)
            .putInt(KEY_SPAM_THRESHOLD, ruleSet.threshold)
            .putString(KEY_SPAM_RULES, encoded)
            .apply()
    }

    fun loadSpamRuleSet(default: SpamRuleSet): SpamRuleSet {
        val encoded = prefs.getString(KEY_SPAM_RULES, null) ?: return default
        val rules = encoded.lineSequence().mapNotNull(::decodeRule).toList()
        if (rules.isEmpty()) return default
        return SpamRuleSet(
            version = prefs.getInt(KEY_SPAM_VERSION, default.version),
            threshold = prefs.getInt(KEY_SPAM_THRESHOLD, default.threshold),
            rules = rules,
        )
    }

    fun isBlockedSender(address: String): Boolean = prefs.getStringSet(KEY_BLOCKED_SENDERS, emptySet()).orEmpty().contains(address)

    fun setBlockedSender(address: String, blocked: Boolean) {
        val current = prefs.getStringSet(KEY_BLOCKED_SENDERS, emptySet()).orEmpty().toMutableSet()
        if (blocked) current.add(address) else current.remove(address)
        prefs.edit().putStringSet(KEY_BLOCKED_SENDERS, current).apply()
    }

    fun syncCursor(): String? = prefs.getString(KEY_SYNC_CURSOR, null)

    fun saveSyncCursor(cursor: String?) {
        prefs.edit().apply { if (cursor == null) remove(KEY_SYNC_CURSOR) else putString(KEY_SYNC_CURSOR, cursor) }.apply()
    }

    private fun decodeRule(encoded: String): SpamRule? {
        val parts = encoded.split('|')
        if (parts.size != 5) return null
        return runCatching {
            SpamRule(
                id = Uri.decode(parts[0]),
                type = SpamRuleType.valueOf(parts[1]),
                pattern = Uri.decode(parts[2]).ifBlank { null },
                score = parts[3].toInt(),
                enabled = parts[4].toBoolean(),
            )
        }.getOrNull()
    }

    companion object {
        private const val PREFS_NAME = "kheyr_prefs"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_SYNC_OPT_IN_SKIPPED = "sync_opt_in_skipped"
        private const val KEY_THEME = "theme"
        private const val KEY_DEFAULT_SUB = "default_subscription_id"
        private const val KEY_DIRECT_MESSAGES = "direct_messages_enabled"
        private const val KEY_SPAM_AUTO_DELETE_DAYS = "spam_auto_delete_days"
        private const val KEY_NOTIF_CONTENT = "notif_content_mode"
        private const val KEY_UNKNOWN_SENDER = "unknown_sender_mode"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_GLOBAL_RINGTONE = "global_ringtone"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_SPAM_VERSION = "spam_version"
        private const val KEY_SPAM_THRESHOLD = "spam_threshold"
        private const val KEY_SPAM_RULES = "spam_rules"
        private const val KEY_BLOCKED_SENDERS = "blocked_senders"
        private const val KEY_SYNC_CURSOR = "sync_cursor"
    }
}
