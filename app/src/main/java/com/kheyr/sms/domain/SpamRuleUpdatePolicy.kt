package com.kheyr.sms.domain

/** Validates downloaded global spam rules before replacing the active local version. */
class SpamRuleUpdatePolicy {
    fun evaluate(current: SpamRuleSet?, candidate: SpamRuleSet): SpamRuleUpdateDecision {
        if (candidate.version <= (current?.version ?: 0)) return SpamRuleUpdateDecision.RejectOlderOrSameVersion
        if (candidate.threshold <= 0) return SpamRuleUpdateDecision.RejectInvalid("Threshold must be positive")
        if (candidate.rules.none { it.enabled }) return SpamRuleUpdateDecision.RejectInvalid("At least one enabled rule is required")
        val invalidRule = candidate.rules.firstOrNull { it.id.isBlank() || it.score == 0 || it.requiresPattern() && it.pattern.isNullOrBlank() }
        return if (invalidRule == null) SpamRuleUpdateDecision.Accept else SpamRuleUpdateDecision.RejectInvalid("Invalid rule: ${invalidRule.id}")
    }

    private fun SpamRule.requiresPattern(): Boolean = type != SpamRuleType.UrlDetected && type != SpamRuleType.SenderNotInContacts && type != SpamRuleType.ShortCode
}

sealed interface SpamRuleUpdateDecision {
    data object Accept : SpamRuleUpdateDecision
    data object RejectOlderOrSameVersion : SpamRuleUpdateDecision
    data class RejectInvalid(val reason: String) : SpamRuleUpdateDecision
}
