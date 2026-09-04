package com.kheyr.sms.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ported from the deleted `spam.SpamClassificationThresholdsTest`: the rule set's configured threshold,
 * not a hard-coded constant, decides where Suspicious ends and Spam begins.
 */
class SpamThresholdTest {
    @Test fun honorsConfiguredThreshold() {
        assertEquals(SpamClassification.Suspicious, classify(score = 80, threshold = 90))
        assertEquals(SpamClassification.Spam, classify(score = 95, threshold = 90))
    }

    @Test fun scoreBelowSuspiciousFloorIsNormal() {
        assertEquals(SpamClassification.Normal, classify(score = 10, threshold = 90))
    }

    private fun classify(score: Int, threshold: Int): SpamClassification = SpamScorer(
        SpamRuleSet(
            version = 1,
            threshold = threshold,
            rules = listOf(SpamRule("keyword", SpamRuleType.MessageKeyword, "buy", score)),
        ),
    ).score(sender = "+15551234567", body = "buy now", senderIsContact = false).classification
}
