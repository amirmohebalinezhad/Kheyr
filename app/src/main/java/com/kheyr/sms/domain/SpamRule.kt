package com.kheyr.sms.domain

import kotlin.math.abs

enum class SpamRuleType {
    SenderExact,
    SenderContains,
    NumberPrefix,
    MessageKeyword,
    MessageRegex,
    UrlDetected,
    SuspiciousLinkPattern,
    SenderNotInContacts,
    OtpRegex,
    KnownSafeSender,
    ShortCode,
}

data class SpamRule(
    val id: String,
    val type: SpamRuleType,
    val pattern: String? = null,
    val score: Int,
    val enabled: Boolean = true,
    /**
     * What kind of message this rule identifies - "financial", "otp", "advertising" and so on - or
     * null for a rule that only carries a spam weight.
     *
     * Independent of [score] by design: the spam axis asks whether a message is unwanted, the
     * category axis asks what it is. A bank SMS is a confident "financial" with a strongly negative
     * [score], and collapsing the two into one number is what makes that inexpressible.
     */
    val category: String? = null,
    /**
     * Confidence in [category]. Zero means the publisher omitted it, in which case [categoryWeight]
     * substitutes the magnitude of [score] - never zero, or a message matching several categories
     * would resolve on whichever rule happened to come first in the pack.
     */
    val categoryScore: Int = 0,
) {
    val categoryWeight: Int get() = if (categoryScore != 0) categoryScore else abs(score)
}

data class SpamRuleSet(
    val version: Int,
    val threshold: Int = 70,
    val rules: List<SpamRule>,
)

data class SpamScore(
    val total: Int,
    val triggeredRuleIds: List<String>,
    val classification: SpamClassification,
    /** Accumulated [SpamRule.categoryWeight] per category across every rule that matched. */
    val categoryScores: Map<String, Int> = emptyMap(),
    /**
     * The rule-derived category: the highest-scoring entry in [categoryScores], ties broken by pack
     * order so the outcome is deterministic. This is only step 3 of the precedence in the plan - a
     * user assignment on the sender or the thread outranks it, and a contact falls back to personal.
     */
    val category: String? = null,
)

enum class SpamClassification { Normal, Suspicious, Spam }
