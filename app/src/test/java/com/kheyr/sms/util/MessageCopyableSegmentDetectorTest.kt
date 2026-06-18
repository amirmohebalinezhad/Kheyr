package com.kheyr.sms.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageCopyableSegmentDetectorTest {
    @Test
    fun findsPersianPhoneWithSeparators() {
        val segments = MessageCopyableSegmentDetector.findAll("تماس: ۰۹۱۲-۳۴۵-۶۷۸۹")
        assertEquals(1, segments.size)
        assertEquals(CopyableSegmentKind.Phone, segments.first().kind)
        assertEquals("0912-345-6789", segments.first().normalized)
    }

    @Test
    fun findsIbanWithSpaces() {
        val segments = MessageCopyableSegmentDetector.findAll("شبا: IR12 3456 7890 1234 5678 9012 34")
        assertEquals(1, segments.size)
        assertEquals(CopyableSegmentKind.Iban, segments.first().kind)
        assertEquals("123456789012345678901234", segments.first().normalized)
    }

    @Test
    fun findsCardNumberWithDashes() {
        val segments = MessageCopyableSegmentDetector.findAll("کارت: ۶۰۳۷-۹۹۱۲-۳۴۵۶-۷۸۹۰")
        assertEquals(1, segments.size)
        assertEquals(CopyableSegmentKind.CardNumber, segments.first().kind)
        assertEquals("6037991234567890", segments.first().normalized)
    }

    @Test
    fun findsCardNumberWithUnderscores() {
        val segments = MessageCopyableSegmentDetector.findAll("کارت: 6037_9912_3456_7890")
        assertEquals(1, segments.size)
        assertEquals(CopyableSegmentKind.CardNumber, segments.first().kind)
        assertEquals("6037991234567890", segments.first().normalized)
    }

    @Test
    fun findsMultipleDistinctNumbers() {
        val body = "تلفن 0912 345 6789 و شبا IR12 3456 7890 1234 5678 9012 34"
        val segments = MessageCopyableSegmentDetector.findAll(body)
        assertEquals(2, segments.size)
        assertTrue(segments.any { it.kind == CopyableSegmentKind.Phone })
        assertTrue(segments.any { it.kind == CopyableSegmentKind.Iban })
        assertEquals("123456789012345678901234", segments.single { it.kind == CopyableSegmentKind.Iban }.normalized)
    }
}
