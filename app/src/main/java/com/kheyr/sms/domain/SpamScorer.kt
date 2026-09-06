package com.kheyr.sms.domain

import com.kheyr.sms.util.PersianTextNormalizer

/**
 * Scores one message against a rule set.
 *
 * Immutable and safe to share: every regex in [ruleSet] is compiled once here, in the constructor,
 * so [score] only reads. That matters because the instance is now reused across incoming messages
 * (see [SpamScorerCache]) and the receiver handles each broadcast on its own thread.
 */
class SpamScorer(private val ruleSet: SpamRuleSet) {
    /**
     * Every pattern compiled up front, invalid ones mapped to null so a single bad regex published
     * to the device disables its own rule instead of throwing on the receive path.
     */
    private val compiledPatterns: Map<String, Regex?> = ruleSet.rules
        .filter { it.type == SpamRuleType.MessageRegex || it.type == SpamRuleType.OtpRegex }
        .associate { rule -> rule.id to compile(rule.pattern) }

    fun score(sender: String, body: String, senderIsContact: Boolean): SpamScore {
        // Once per message, before any rule runs: the rules are written against normalized text.
        val normalizedBody = PersianTextNormalizer.normalize(body)
        val safeRule = ruleSet.rules.firstOrNull {
            it.enabled && it.type == SpamRuleType.KnownSafeSender && matches(it, sender, normalizedBody, senderIsContact)
        }
        if (safeRule != null) {
            return SpamScore(0, listOf(safeRule.id), SpamClassification.Normal)
        }
        var total = 0
        val triggered = mutableListOf<String>()
        ruleSet.rules.filter { it.enabled }.forEach { rule ->
            if (matches(rule, sender, normalizedBody, senderIsContact)) {
                total += rule.score
                triggered += rule.id
            }
        }
        val classification = when {
            total >= ruleSet.threshold -> SpamClassification.Spam
            total >= SUSPICIOUS_THRESHOLD -> SpamClassification.Suspicious
            else -> SpamClassification.Normal
        }
        return SpamScore(total, triggered, classification)
    }

    private fun matches(rule: SpamRule, sender: String, body: String, senderIsContact: Boolean): Boolean = when (rule.type) {
        SpamRuleType.SenderExact -> rule.pattern?.equals(sender, ignoreCase = true) == true
        SpamRuleType.SenderContains -> rule.pattern?.let { sender.contains(it, ignoreCase = true) } == true
        SpamRuleType.NumberPrefix -> rule.pattern?.let { sender.startsWith(it) } == true
        SpamRuleType.MessageKeyword -> rule.pattern?.let { body.contains(it, ignoreCase = true) } == true
        SpamRuleType.MessageRegex, SpamRuleType.OtpRegex -> compiledPatterns[rule.id]?.containsMatchIn(body) == true
        SpamRuleType.UrlDetected -> urlRegex.containsMatchIn(body)
        SpamRuleType.SuspiciousLinkPattern -> suspiciousLinkRegex.containsMatchIn(body)
        SpamRuleType.SenderNotInContacts -> !senderIsContact
        SpamRuleType.KnownSafeSender -> rule.pattern?.equals(sender, ignoreCase = true) == true
        SpamRuleType.ShortCode -> sender.all(Char::isDigit) && sender.length in 3..6
    }

    companion object {
        /**
         * Lower bound of [SpamClassification.Suspicious]. The Spam cut is per-rule-set
         * (`ruleSet.threshold`), but this one is fixed: the rule weights are tuned so that an
         * advertising message lands on exactly 40, and moving it would re-band the whole pack.
         */
        const val SUSPICIOUS_THRESHOLD = 40

        private val urlRegex = Regex("https?://|www\\.", RegexOption.IGNORE_CASE)
        private val suspiciousLinkRegex = Regex("https?://[^\\s]*(bit\\.ly|tinyurl|t\\.co|free|winner|prize)[^\\s]*", RegexOption.IGNORE_CASE)

        private fun compile(pattern: String?): Regex? = pattern?.let {
            try {
                it.toRegex(RegexOption.IGNORE_CASE)
            } catch (t: Throwable) {
                null
            }
        }
    }
}
