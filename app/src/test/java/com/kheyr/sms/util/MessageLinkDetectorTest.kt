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

    @Test
    fun findsBareDomainWithPath() {
        val links = MessageLinkDetector.findAll("Open example.com/foo/bar for details")
        assertEquals(1, links.size)
        assertEquals("https://example.com/foo/bar", links.first().url)
        assertEquals(5, links.first().start)
        assertEquals(24, links.first().end)
    }

    @Test
    fun findsBareDomainWithHyphensAndDigits() {
        val links = MessageLinkDetector.findAll("site2.my-site.io/page")
        assertEquals(1, links.size)
        assertEquals("https://site2.my-site.io/page", links.first().url)
    }

    @Test
    fun doesNotDuplicateLinkInsideHttpUrl() {
        val links = MessageLinkDetector.findAll("Visit https://example.com/path now")
        assertEquals(1, links.size)
        assertEquals("https://example.com/path", links.first().url)
    }
}
