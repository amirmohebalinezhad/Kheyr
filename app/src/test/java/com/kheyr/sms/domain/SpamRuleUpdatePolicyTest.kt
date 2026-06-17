package com.kheyr.sms.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpamRuleUpdatePolicyTest {
    private val policy = SpamRuleUpdatePolicy()

    @Test fun acceptsNewerValidRuleSet() {
        assertEquals(SpamRuleUpdateDecision.Accept, policy.evaluate(current = null, candidate = rules(version = 2)))
    }

    @Test fun rejectsOlderOrSameVersionRuleSet() {
        assertEquals(SpamRuleUpdateDecision.RejectOlderOrSameVersion, policy.evaluate(rules(version = 2), rules(version = 2)))
    }

    @Test fun rejectsInvalidRulePayloads() {
        val invalid = rules(version = 3).copy(rules = listOf(SpamRule("keyword", SpamRuleType.MessageKeyword, pattern = "", score = 10)))
        assertTrue(policy.evaluate(rules(version = 2), invalid) is SpamRuleUpdateDecision.RejectInvalid)
    }

    @Test fun acceptsPatternlessSuspiciousLinkRule() {
        val suspiciousLinkRule = SpamRule("suspicious-link", SpamRuleType.SuspiciousLinkPattern, score = 45)
        val candidate = rules(version = 3).copy(rules = listOf(suspiciousLinkRule))
        assertEquals(SpamRuleUpdateDecision.Accept, policy.evaluate(rules(version = 2), candidate))
    }

    private fun rules(version: Int) = SpamRuleSet(version, threshold = 70, rules = listOf(SpamRule("url", SpamRuleType.UrlDetected, score = 40)))
}
