package com.kheyr.sms.spam

import android.content.Context
import com.kheyr.sms.domain.SpamClassification
import com.kheyr.sms.domain.SpamRule
import com.kheyr.sms.domain.SpamRuleSet
import com.kheyr.sms.domain.SpamRuleType
import com.kheyr.sms.domain.SpamScorer
import com.kheyr.sms.preferences.AppPreferences
import com.kheyr.sms.receiver.PreferencesSpamRulesProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BundledSpamRuleSetTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun bundledAssetContainsTheFullPersianPack() {
        val ruleSet = BundledSpamRuleSet.load(context)

        assertEquals(BundledSpamRuleSet.VERSION, ruleSet.version)
        assertEquals(BundledSpamRuleSet.THRESHOLD, ruleSet.threshold)
        assertEquals(21, ruleSet.rules.size)
        assertEquals(21, ruleSet.rules.map { it.id }.toSet().size)
        assertTrue(ruleSet.rules.all { it.type == SpamRuleType.MessageRegex })
        assertTrue(ruleSet.rules.any { it.id == "fa-otp" && it.category == "otp" && it.categoryScore == 70 })
        assertTrue(ruleSet.rules.any { it.id == "fa-bulk-ad-composite" && it.category == "advertising" })
        assertTrue(ruleSet.rules.any { it.id == "fa-vas-bait" && it.score == 45 })
    }

    @Test
    fun bundledPackProtectsOtpMessages() {
        val scorer = SpamScorer(BundledSpamRuleSet.load(context))

        val score = scorer.score(
            sender = "20001",
            body = "کد ورود شما: ۱۲۳۴۵۶",
            senderIsContact = false,
        )

        assertEquals(SpamClassification.Normal, score.classification)
        assertEquals("otp", score.category)
        assertTrue("fa-otp must be active from the bundled asset", "fa-otp" in score.triggeredRuleIds)
    }

    @Test
    fun olderCachedSeedCannotHideTheBundledPack() {
        val preferences = AppPreferences(context)
        preferences.saveSpamRuleSet(
            SpamRuleSet(
                version = 1,
                threshold = 70,
                rules = listOf(SpamRule("old-seed", SpamRuleType.MessageKeyword, "winner", 30)),
            ),
        )

        val provider = PreferencesSpamRulesProvider(preferences, BundledSpamRuleSet.load(context))

        assertEquals(2, provider.activeRuleSet().version)
        assertEquals(21, provider.activeRuleSet().rules.size)
    }

    @Test
    fun emptyOrMalformedPayloadIsRejected() {
        assertNull(BundledSpamRuleSet.parse(null))
        assertNull(BundledSpamRuleSet.parse("[]"))
        assertNull(BundledSpamRuleSet.parse("not json"))
    }
}
