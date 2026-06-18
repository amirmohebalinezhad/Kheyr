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
        assertEquals("IR12 3456 7890 1234 5678 9012 34", segments.first().normalized)
    }

    @Test
    fun findsCardNumberWithDashes() {
        val segments = MessageCopyableSegmentDetector.findAll("کارت: ۶۰۳۷-۹۹۱۲-۳۴۵۶-۷۸۹۰")
        assertEquals(1, segments.size)
        assertEquals(CopyableSegmentKind.CardNumber, segments.first().kind)
        assertEquals("6037-9912-3456-7890", segments.first().normalized)
    }

    @Test
    fun findsMultipleDistinctNumbers() {
        val body = "تلفن 0912 345 6789 و شبا IR1201234567890123456789012"
        val segments = MessageCopyableSegmentDetector.findAll(body)
        assertEquals(2, segments.size)
        assertTrue(segments.any { it.kind == CopyableSegmentKind.Phone })
        assertTrue(segments.any { it.kind == CopyableSegmentKind.Iban })
    }
}
