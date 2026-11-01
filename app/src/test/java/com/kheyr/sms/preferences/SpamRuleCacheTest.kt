package com.kheyr.sms.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.kheyr.sms.domain.SpamRule
import com.kheyr.sms.domain.SpamRuleSet
import com.kheyr.sms.domain.SpamRuleType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The cached rule set is a hand-rolled pipe-joined encoding, and decodeRule silently drops anything
 * that does not parse. That makes a field-layout change the dangerous kind of change: without the
 * format stamp, an old cache would come back as a partial rule set with nothing reported.
 */
@RunWith(RobolectricTestRunner::class)
class SpamRuleCacheTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferences = AppPreferences(context)

    private val defaults = SpamRuleSet(
        version = 1,
        threshold = 70,
        rules = listOf(SpamRule("fallback", SpamRuleType.UrlDetected, score = 35)),
    )

    @Test fun categoryFieldsSurviveTheCacheRoundTrip() {
        val published = SpamRuleSet(
            version = 7,
            threshold = 70,
            rules = listOf(
                SpamRule("fa-otp", SpamRuleType.MessageRegex, "کد تایید", -70, category = "otp", categoryScore = 70),
                SpamRule("fa-laghv-prior", SpamRuleType.MessageRegex, "لغو11", 5, category = "advertising", categoryScore = 5),
            ),
        )

        preferences.saveSpamRuleSet(published)

        assertEquals(published, preferences.loadSpamRuleSet(defaults))
    }

    @Test fun regexAlternationSurvivesTheCacheRoundTrip() {
        // The encoding joins on '|', which is also regex alternation. Uri.encode percent-encodes it,
        // so the pattern comes back intact - the property the whole format depends on.
        val pattern = "تخفیف|حراج|جشنواره"
        val published = SpamRuleSet(2, 70, listOf(SpamRule("promo", SpamRuleType.MessageRegex, pattern, 30, category = "advertising", categoryScore = 30)))

        preferences.saveSpamRuleSet(published)

        assertEquals(pattern, preferences.loadSpamRuleSet(defaults).rules.single().pattern)
    }

    @Test fun aRuleWithoutACategoryRoundTripsAsNull() {
        val published = SpamRuleSet(2, 70, listOf(SpamRule("plain", SpamRuleType.MessageRegex, "خبر", 10)))

        preferences.saveSpamRuleSet(published)

        val loaded = preferences.loadSpamRuleSet(defaults).rules.single()
        assertEquals(null, loaded.category)
        assertEquals(0, loaded.categoryScore)
    }

    @Test fun aCacheInTheOldFieldLayoutIsDiscardedWholesale() {
        // Exactly what a build before the category axis wrote: five fields and no format stamp.
        context.getSharedPreferences("kheyr_prefs", Context.MODE_PRIVATE).edit()
            .putInt("spam_version", 6)
            .putInt("spam_threshold", 70)
            .putString("spam_rules", "fa-otp|MessageRegex|%DA%A9%D8%AF|-70|true")
            .apply()

        assertEquals(
            "an unversioned cache must fall back to the defaults, not to a partial rule set",
            defaults,
            preferences.loadSpamRuleSet(defaults),
        )
    }
}
