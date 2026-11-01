package com.kheyr.sms.spam

import android.content.Context
import com.kheyr.sms.domain.SpamRule
import com.kheyr.sms.domain.SpamRuleSet
import com.kheyr.sms.domain.SpamRuleType
import org.json.JSONArray
import org.json.JSONObject

/**
 * The Persian rule pack that ships inside the APK.
 *
 * The live pack is normally downloaded from `/api/v1/spam-rules/latest` and cached in
 * [com.kheyr.sms.preferences.AppPreferences]. A fresh install, a phone that has never reached the
 * backend, or a backend still serving the old seed therefore used to classify messages with only
 * the six generic fallback rules. Bundling the v2 pack makes the real Persian rules active offline
 * on first launch; a newer server-published version still replaces it through [SpamRulesWorker]
 * and [com.kheyr.sms.domain.SpamRuleUpdatePolicy].
 */
object BundledSpamRuleSet {
    const val ASSET_NAME = "spam-rules-fa-v2.json"

    /** Must be bumped whenever the bundled file changes. */
    const val VERSION = 2
    const val THRESHOLD = 70

    @Volatile
    private var cached: SpamRuleSet? = null

    /**
     * Last-resort rules used only if the bundled asset is missing or unreadable. These are the
     * historical six generic rules, not the Persian pack.
     */
    val fallback: SpamRuleSet = SpamRuleSet(
        version = 1,
        threshold = THRESHOLD,
        rules = listOf(
            SpamRule("spam-keyword-winner", SpamRuleType.MessageKeyword, "winner", 35),
            SpamRule("spam-keyword-prize", SpamRuleType.MessageKeyword, "prize", 35),
            SpamRule("spam-url", SpamRuleType.UrlDetected, score = 35),
            SpamRule("spam-suspicious-link", SpamRuleType.SuspiciousLinkPattern, score = 45),
            SpamRule("unknown-sender", SpamRuleType.SenderNotInContacts, score = 25),
            SpamRule(
                "otp-code",
                SpamRuleType.OtpRegex,
                "\\b(code|otp)\\b.*\\b\\d{4,8}\\b|\\b\\d{4,8}\\b.*\\b(code|otp)\\b",
                -50,
            ),
        ),
    )

    fun load(context: Context): SpamRuleSet {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: runCatching {
                context.applicationContext.assets.open(ASSET_NAME)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
            }.getOrNull().let(::parse)?.also { cached = it } ?: fallback
        }
    }

    /** Visible for tests; returns null when the payload has no usable rules. */
    internal fun parse(json: String?): SpamRuleSet? {
        if (json.isNullOrBlank()) return null
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return null
        val rules = (0 until array.length()).mapNotNull { index -> parseRule(array.optJSONObject(index)) }
        if (rules.isEmpty()) return null
        return SpamRuleSet(version = VERSION, threshold = THRESHOLD, rules = rules)
    }

    private fun parseRule(json: JSONObject?): SpamRule? {
        if (json == null) return null
        val id = text(json, "Id", "id") ?: return null
        val type = ruleType(text(json, "Type", "type")) ?: return null
        val score = integer(json, "Score", "score") ?: return null
        if (score == 0 && type != SpamRuleType.KnownSafeSender) return null
        val pattern = text(json, "Pattern", "pattern")
        if (pattern == null && type.requiresPattern()) return null
        return SpamRule(
            id = id,
            type = type,
            pattern = pattern,
            score = score,
            enabled = boolean(json, "Enabled", "enabled") ?: true,
            category = text(json, "Category", "category"),
            categoryScore = integer(json, "CategoryScore", "category_score") ?: 0,
        )
    }

    private fun text(json: JSONObject, vararg keys: String): String? {
        keys.forEach { key ->
            if (json.has(key) && !json.isNull(key)) {
                val value = json.optString(key)
                if (value.isNotBlank() && value != "null") return value
            }
        }
        return null
    }

    private fun integer(json: JSONObject, vararg keys: String): Int? {
        keys.forEach { key ->
            if (json.has(key) && !json.isNull(key)) {
                return runCatching { json.getInt(key) }.getOrNull()
            }
        }
        return null
    }

    private fun boolean(json: JSONObject, vararg keys: String): Boolean? {
        keys.forEach { key ->
            if (json.has(key) && !json.isNull(key)) return json.optBoolean(key)
        }
        return null
    }

    private fun ruleType(wire: String?): SpamRuleType? {
        if (wire.isNullOrBlank()) return null
        val enumName = wire.split('_').joinToString("") { part ->
            part.replaceFirstChar { it.uppercaseChar() }
        }
        return SpamRuleType.entries.firstOrNull { it.name == enumName }
    }

    private fun SpamRuleType.requiresPattern(): Boolean = this != SpamRuleType.UrlDetected &&
        this != SpamRuleType.SenderNotInContacts &&
        this != SpamRuleType.ShortCode &&
        this != SpamRuleType.SuspiciousLinkPattern
}
