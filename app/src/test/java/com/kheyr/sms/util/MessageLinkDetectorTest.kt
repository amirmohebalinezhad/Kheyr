package com.kheyr.sms.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageLinkDetectorTest {
    @Test
    fun findsHttpAndWwwLinks() {
        val links = MessageLinkDetector.findAll("Visit https://example.com or www.test.org today.")
        assertEquals(2, links.size)
        assertEquals("https://example.com", links[0].url)
        assertEquals("https://www.test.org", links[1].url)
    }

    @Test
    fun trimsTrailingPunctuation() {
        val links = MessageLinkDetector.findAll("See https://example.com.")
        assertEquals(1, links.size)
        assertEquals("https://example.com", links.first().url)
    }
}
