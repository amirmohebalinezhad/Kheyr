package com.kheyr.sms.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpDetectorTest {
    @Test
    fun findsLabeledCode() {
        assertEquals("123456", OtpDetector.findCopyableCode("Your verification code is 123456"))
        assertEquals("4321", OtpDetector.findCopyableCode("کد شما: 4321"))
    }

    @Test
    fun findsSingleStandaloneCode() {
        assertEquals("9876", OtpDetector.findCopyableCode("Use 9876 to login"))
    }

    @Test
    fun findsStandaloneArabicIndicCode() {
        assertEquals("1234", OtpDetector.findCopyableCode("Use ١٢٣٤ to login"))
    }

    @Test
    fun findsStandalonePersianCode() {
        assertEquals("9876", OtpDetector.findCopyableCode("ورود با ۹۸۷۶"))
    }

    @Test
    fun ignoresMultipleCodes() {
        assertNull(OtpDetector.findCopyableCode("Codes 1234 and 5678 are invalid"))
    }
}
