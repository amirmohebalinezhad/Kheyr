package com.kheyr.sms.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The category axis is independent of the spam axis: a bank SMS is a confident "financial" and a
 * strongly negative spam score at the same time, which one number cannot express.
 */
class SpamScorerCategoryTest {
    @Test fun categoryIsTheHighestAccumulatedCategoryScore() {
        val scorer = SpamScorer(
            SpamRuleSet(
                version = 2,
                rules = listOf(
                    rule("money", "واریز", score = -55, category = "financial", categoryScore = 55),
                    rule("card", "کارت", score = -35, category = "financial", categoryScore = 35),
                    rule("promo", "تخفیف", score = 30, category = "advertising", categoryScore = 30),
                ),
            ),
        )

        val score = scorer.score("BANK", "واریز به کارت شما با تخفیف", senderIsContact = false)

        assertEquals(mapOf("financial" to 90, "advertising" to 30), score.categoryScores)
        assertEquals("financial", score.category)
        assertEquals("the spam axis is unaffected by the category axis", -60, score.total)
    }

    @Test fun aRuleWithoutACategoryScoreCountsAsTheMagnitudeOfItsScore() {
        // Never zero: a rule that omits the field must still be able to decide a category.
        val scorer = SpamScorer(
            SpamRuleSet(
                version = 2,
                rules = listOf(rule("otp", "رمز", score = -70, category = "otp", categoryScore = 0)),
            ),
        )

        val score = scorer.score("20001", "رمز پویا", senderIsContact = false)

        assertEquals(mapOf("otp" to 70), score.categoryScores)
        assertEquals("otp", score.category)
    }

    @Test fun knownSafeSenderStillResolvesACategory() {
        // The safe-sender short circuit zeroes the spam score. If it also skipped the rule pass,
        // every trusted sender would lose categorization entirely.
        val scorer = SpamScorer(
            SpamRuleSet(
                version = 2,
                rules = listOf(
                    SpamRule("safe-bank", SpamRuleType.KnownSafeSender, "BANK", score = 0),
                    rule("money", "واریز", score = -55, category = "financial", categoryScore = 55),
                    rule("promo", "تخفیف", score = 30, category = "advertising", categoryScore = 30),
                ),
            ),
        )

        val score = scorer.score("BANK", "واریز با تخفیف ویژه", senderIsContact = false)

        assertEquals("the safe sender still zeroes the spam axis", 0, score.total)
        assertEquals(listOf("safe-bank"), score.triggeredRuleIds)
        assertEquals(SpamClassification.Normal, score.classification)
        assertEquals("financial", score.category)
        assertEquals(mapOf("financial" to 55, "advertising" to 30), score.categoryScores)
    }

    @Test fun tiesResolveOnPackOrderSoTheOutcomeIsDeterministic() {
        val rules = listOf(
            rule("first", "الف", score = 20, category = "delivery", categoryScore = 20),
            rule("second", "ب", score = 20, category = "advertising", categoryScore = 20),
        )
        val forward = SpamScorer(SpamRuleSet(2, rules = rules)).score("1", "الف ب", false)
        val reversed = SpamScorer(SpamRuleSet(2, rules = rules.reversed())).score("1", "الف ب", false)

        assertEquals("delivery", forward.category)
        assertEquals("advertising", reversed.category)
    }

    @Test fun aRuleWithoutACategoryContributesNothingToTheCategoryAxis() {
        val scorer = SpamScorer(
            SpamRuleSet(2, rules = listOf(SpamRule("url", SpamRuleType.UrlDetected, score = 35))),
        )

        val score = scorer.score("1", "https://example.ir", false)

        assertTrue(score.categoryScores.isEmpty())
        assertEquals(null, score.category)
    }

    private fun rule(id: String, pattern: String, score: Int, category: String?, categoryScore: Int) =
        SpamRule(id, SpamRuleType.MessageRegex, pattern, score, category = category, categoryScore = categoryScore)
}
