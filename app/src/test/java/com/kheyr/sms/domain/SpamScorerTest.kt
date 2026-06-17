package com.kheyr.sms.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SpamScorerTest {
    @Test fun highScoreMessageIsSpam() {
        val scorer = SpamScorer(
            SpamRuleSet(1, rules = listOf(
                SpamRule("prefix", SpamRuleType.NumberPrefix, "+98999", 35),
                SpamRule("winner", SpamRuleType.MessageKeyword, "winner", 30),
                SpamRule("url", SpamRuleType.UrlDetected, score = 40),
            ))
        )
        val score = scorer.score("+989991234", "winner visit https://example.com", false)
        assertEquals(SpamClassification.Spam, score.classification)
    }

    @Test fun otpRuleCanReduceSpamScore() {
        val scorer = SpamScorer(
            SpamRuleSet(1, rules = listOf(
                SpamRule("unknown", SpamRuleType.SenderNotInContacts, score = 45),
                SpamRule("otp", SpamRuleType.OtpRegex, "\\b\\d{4,8}\\b", -40),
            ))
        )
        val score = scorer.score("12345", "Your code is 123456", false)
        assertEquals(SpamClassification.Normal, score.classification)
    }
}
