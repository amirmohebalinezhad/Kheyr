package com.kheyr.sms.spam

import com.kheyr.sms.domain.SpamClassification
import com.kheyr.sms.domain.SpamRule
import com.kheyr.sms.domain.SpamRuleSet
import com.kheyr.sms.domain.SpamRuleType
import com.kheyr.sms.domain.SpamScorer
import java.io.File
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Guards the published Persian rule pack against the ways it can silently stop working.
 *
 * The pack is authored once and executed by Kotlin/Java on device, so a pattern that is valid in the
 * dialect it was measured with but not in Kotlin's would be dropped by [SpamScorer] with no error
 * surfaced. These tests read the shipped file itself rather than a copy, so the file and the
 * expectations cannot drift apart.
 *
 * Fixtures are synthetic. The measured corpus is private and is deliberately not in the repository.
 */
@RunWith(RobolectricTestRunner::class)
class SpamRulePackTest {
    @Test fun everyPublishedPatternCompilesAsKotlinRegex() {
        val failures = packRules().mapNotNull { rule ->
            val pattern = rule.pattern ?: return@mapNotNull "${rule.id}: no pattern"
            runCatching { Regex(pattern) }.exceptionOrNull()?.let { "${rule.id}: ${it.message}" }
        }
        assertTrue("patterns that do not compile on device: $failures", failures.isEmpty())
        assertEquals("the pack is 21 rules", 21, packRules().size)
    }

    @Test fun everyRuleIsMessageRegexSoTheWireParserKeepsIt() {
        // toRuleTypeOrNull() drops unknown types rather than throwing, so a renamed Type would
        // shrink the rule set on device without any failure being reported.
        val unexpected = packRules().filter { it.type != SpamRuleType.MessageRegex }
        assertTrue("every rule must stay message_regex, got $unexpected", unexpected.isEmpty())
    }

    @Test fun terseAdWithOnlyBulkLineAndContactChannelScoresExactlyForty() {
        // The class the composite rule exists for: an offer, a contact channel, and the opt-out
        // footer, with no discount vocabulary anywhere. +35 and +5 is the whole score.
        val score = score("تور مشهد\nexample.ir\nلغو11")
        assertEquals(
            "only the composite and the opt-out prior may fire",
            listOf("fa-bulk-ad-composite", "fa-laghv-prior"),
            score.triggeredRuleIds.sorted(),
        )
        assertEquals("the pair is tuned to land on the advertising cut exactly", 40, score.total)
        assertEquals(SpamClassification.Suspicious, score.classification)
    }

    @Test fun advertisingBandDoesNotSetSpam() {
        // Ads stay in the inbox: a mixed sender's delivery notices must not be hidden with them.
        val score = score("تور مشهد\nexample.ir\nلغو11")
        assertTrue("advertising must stay below the spam threshold", score.total < 70)
    }

    @Test fun otpMessagesNeverReachTheAdvertisingBand() {
        val otps = listOf(
            "کد تایید: 123456",
            // Persian digits, which Java's \d does not match until normalization folds them.
            "کد تایید: ۱۲۳۴۵۶",
            // The intervening-word form an earlier regex missed; worth 732 OTPs in measurement.
            "کد ورود به سرویس: ۵۱۷۰۷",
            "رمز پویا: 84213",
            // An OTP carried on a bulk line with a link: the composite must refuse to fire on it.
            "کد تایید: 123456 example.ir لغو11",
        )
        otps.forEach { body ->
            val score = score(body)
            assertTrue(
                "OTP must never be classified as advertising: '$body' scored ${score.total} via ${score.triggeredRuleIds}",
                score.total < SpamScorer.SUSPICIOUS_THRESHOLD,
            )
            assertTrue("fa-otp must recognise '$body'", "fa-otp" in score.triggeredRuleIds)
            assertEquals(SpamClassification.Normal, score.classification)
        }
    }

    @Test fun protectedMessagesNeverReachTheAdvertisingBand() {
        listOf(
            "مانده حساب شما 1,250,000 ریال",
            "ابلاغ الکترونیک قوه قضاییه در سامانه ثنا",
            "شماره مرسوله 12345678 ارسال شد",
        ).forEach { body ->
            val score = score(body)
            assertTrue(
                "protected message must stay out of the advertising band: '$body' scored ${score.total}",
                score.total < SpamScorer.SUSPICIOUS_THRESHOLD,
            )
        }
    }

    @Test fun scamBaitReachesTheSpamThresholdOnlyWithSupportingSignals() {
        // fa-vas-bait is +45 on its own: suspicious, not yet spam. The recurring template also
        // carries an opt-out footer and a short number, which is what pushes it over 70.
        assertEquals(45, score("عضویت شما لغو شد").total)
    }

    @Test fun arabicVariantsScoreIdenticallyToTheNormalisedForm() {
        // Without normalization these lose fa-otp entirely: the pattern spells تایید with the
        // Persian yeh, and Java's \d does not match Persian digits.
        val zwnj = Char(0x200C).toString()
        listOf(
            "کد تاييد: ۱۲۳۴۵۶" to "کد تایید: 123456",
            "كد تاييد: ١٢٣٤٥٦" to "کد تایید: 123456",
            "کد فعال" + zwnj + "سازی: ۵۱۷۰۷" to "کد فعالسازی: 51707",
        ).forEach { (raw, normalized) ->
            val rawScore = score(raw)
            val normalizedScore = score(normalized)
            assertEquals(
                "'$raw' must score as its normalised form",
                normalizedScore.total,
                rawScore.total,
            )
            assertEquals(normalizedScore.triggeredRuleIds.sorted(), rawScore.triggeredRuleIds.sorted())
            assertTrue("normalisation must recover fa-otp", "fa-otp" in rawScore.triggeredRuleIds)
        }
    }

    @Test fun adDetectionSurvivesArabicVariantSpelling() {
        val persian = score("تور مشهد\nexample.ir\nلغو11")
        val arabic = score("تور مشهد\nexample.ir\nلغو۱۱")
        assertEquals("Persian-digit opt-out footers must score the same", persian.total, arabic.total)
        assertEquals(40, arabic.total)
    }

    private fun score(body: String) = SpamScorer(SpamRuleSet(version = 2, threshold = 70, rules = packRules()))
        .score(sender = "20001", body = body, senderIsContact = false)

    private companion object {
        private val cached: List<SpamRule> by lazy { parsePack(packFile().readText()) }

        fun packRules(): List<SpamRule> = cached

        /**
         * The pack lives beside the backend that publishes it, not in the app module, so the test
         * walks up from the module directory to the repository root to find it.
         */
        fun packFile(): File {
            var dir: File? = File("").absoluteFile
            while (dir != null) {
                val candidate = File(dir, "backend/spam-rules-fa-v2.json")
                if (candidate.isFile) return candidate
                dir = dir.parentFile
            }
            fail("could not locate backend/spam-rules-fa-v2.json from ${File("").absolutePath}")
            error("unreachable")
        }

        /**
         * The file is PascalCase because that is what the admin publish form binds (case-sensitive
         * System.Text.Json). The device receives the lowercase projection instead, so this mapping
         * is test-only and intentionally mirrors the publishing format, not the wire format.
         */
        fun parsePack(json: String): List<SpamRule> {
            val array = JSONArray(json)
            return (0 until array.length()).map { index ->
                val rule = array.getJSONObject(index)
                SpamRule(
                    id = rule.getString("Id"),
                    type = ruleType(rule.getString("Type")),
                    pattern = rule.optString("Pattern").takeIf { it.isNotBlank() },
                    score = rule.getInt("Score"),
                    enabled = rule.optBoolean("Enabled", true),
                )
            }
        }

        fun ruleType(wire: String): SpamRuleType {
            val enumName = wire.split('_').joinToString("") { part -> part.replaceFirstChar { it.uppercaseChar() } }
            return SpamRuleType.entries.first { it.name == enumName }
        }
    }
}
