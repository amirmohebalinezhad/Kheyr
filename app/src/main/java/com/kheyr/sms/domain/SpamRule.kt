package com.kheyr.sms.domain

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
)

data class SpamRuleSet(
    val version: Int,
    val threshold: Int = 70,
    val rules: List<SpamRule>,
)

data class SpamScore(
    val total: Int,
    val triggeredRuleIds: List<String>,
    val classification: SpamClassification,
)

enum class SpamClassification { Normal, Suspicious, Spam }
