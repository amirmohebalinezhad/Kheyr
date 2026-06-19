package com.kheyr.sms.domain

class SpamScorer(private val ruleSet: SpamRuleSet) {
    fun score(sender: String, body: String, senderIsContact: Boolean): SpamScore {
        val safeRule = ruleSet.rules.firstOrNull {
            it.enabled && it.type == SpamRuleType.KnownSafeSender && matches(it, sender, body, senderIsContact)
        }
        if (safeRule != null) {
            return SpamScore(0, listOf(safeRule.id), SpamClassification.Normal)
        }
        var total = 0
        val triggered = mutableListOf<String>()
        ruleSet.rules.filter { it.enabled }.forEach { rule ->
            if (matches(rule, sender, body, senderIsContact)) {
                total += rule.score
                triggered += rule.id
            }
        }
        val classification = when {
            total >= ruleSet.threshold -> SpamClassification.Spam
            total >= 40 -> SpamClassification.Suspicious
            else -> SpamClassification.Normal
        }
        return SpamScore(total, triggered, classification)
    }

    private fun matches(rule: SpamRule, sender: String, body: String, senderIsContact: Boolean): Boolean = when (rule.type) {
        SpamRuleType.SenderExact -> rule.pattern?.equals(sender, ignoreCase = true) == true
        SpamRuleType.SenderContains -> rule.pattern?.let { sender.contains(it, ignoreCase = true) } == true
        SpamRuleType.NumberPrefix -> rule.pattern?.let { sender.startsWith(it) } == true
        SpamRuleType.MessageKeyword -> rule.pattern?.let { body.contains(it, ignoreCase = true) } == true
        SpamRuleType.MessageRegex, SpamRuleType.OtpRegex -> rule.pattern?.toRegex(RegexOption.IGNORE_CASE)?.containsMatchIn(body) == true
        SpamRuleType.UrlDetected -> urlRegex.containsMatchIn(body)
        SpamRuleType.SuspiciousLinkPattern -> suspiciousLinkRegex.containsMatchIn(body)
        SpamRuleType.SenderNotInContacts -> !senderIsContact
        SpamRuleType.KnownSafeSender -> rule.pattern?.equals(sender, ignoreCase = true) == true
        SpamRuleType.ShortCode -> sender.all(Char::isDigit) && sender.length in 3..6
    }

    private companion object {
        val urlRegex = Regex("https?://|www\\.", RegexOption.IGNORE_CASE)
        val suspiciousLinkRegex = Regex("https?://[^\\s]*(bit\\.ly|tinyurl|t\\.co|free|winner|prize)[^\\s]*", RegexOption.IGNORE_CASE)
    }
}
