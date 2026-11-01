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
        var total = 0
        val triggered = mutableListOf<String>()
        // Insertion-ordered so that ties resolve on pack order rather than on hash order.
        val categoryScores = LinkedHashMap<String, Int>()
        var safeRuleId: String? = null

        // One pass over the rules accumulates both axes. A KnownSafeSender match zeroes the spam
        // axis but must not stop the category axis, or safe senders lose categorization entirely.
        ruleSet.rules.forEach { rule ->
            if (!rule.enabled || !matches(rule, sender, normalizedBody, senderIsContact)) return@forEach
            if (rule.type == SpamRuleType.KnownSafeSender && safeRuleId == null) safeRuleId = rule.id
            total += rule.score
            triggered += rule.id
            rule.category?.let { category ->
                categoryScores[category] = (categoryScores[category] ?: 0) + rule.categoryWeight
            }
        }
        // maxByOrNull keeps the first entry on a tie, and the map preserves pack order.
        val category = categoryScores.maxByOrNull { it.value }?.key

        val safeRule = safeRuleId
        if (safeRule != null) {
            return SpamScore(0, listOf(safeRule), SpamClassification.Normal, categoryScores, category)
        }
        val classification = when {
            total >= ruleSet.threshold -> SpamClassification.Spam
            total >= SUSPICIOUS_THRESHOLD -> SpamClassification.Suspicious
            else -> SpamClassification.Normal
        }
        return SpamScore(total, triggered, classification, categoryScores, category)
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
