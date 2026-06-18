package com.kheyr.sms.spam

import com.kheyr.sms.domain.SpamRule
import com.kheyr.sms.domain.SpamRuleSet
import com.kheyr.sms.domain.SpamRuleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpamRuleDownloaderTest {
    private val downloader = SpamRuleDownloader()

    @Test fun acceptsNewerValidRuleSet() {
        val result = downloader.validate(current = rules(version = 1), candidate = rules(version = 2))
        assertTrue(result.accepted)
        assertEquals(2, result.ruleSet?.version)
    }

    @Test fun rejectsOlderOrSameVersion() {
        val result = downloader.validate(current = rules(version = 5), candidate = rules(version = 5))
        assertFalse(result.accepted)
        assertNull(result.ruleSet)
        assertEquals("older_or_same_version", result.error)
    }

    @Test fun rejectsInvalidPayload() {
        val invalid = rules(version = 9).copy(rules = listOf(SpamRule("kw", SpamRuleType.MessageKeyword, pattern = "", score = 10)))
        val result = downloader.validate(current = rules(version = 1), candidate = invalid)
        assertFalse(result.accepted)
        assertNull(result.ruleSet)
    }

    @Test fun rejectsNullCandidate() {
        val result = downloader.validate(current = rules(version = 1), candidate = null)
        assertFalse(result.accepted)
        assertEquals("no_rules", result.error)
    }

    private fun rules(version: Int) = SpamRuleSet(version, threshold = 70, rules = listOf(SpamRule("url", SpamRuleType.UrlDetected, score = 40)))
}
