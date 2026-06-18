package com.kheyr.sms.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DigitNormalizerTest {
    @Test
    fun convertsPersianDigitsToAscii() {
        assertEquals("123456", DigitNormalizer.toAsciiDigits("۱۲۳۴۵۶"))
    }

    @Test
    fun convertsArabicDigitsToAscii() {
        assertEquals("7890", DigitNormalizer.toAsciiDigits("٧٨٩٠"))
    }

    @Test
    fun preservesSeparators() {
        assertEquals("09-12 345 6789", DigitNormalizer.toAsciiDigits("۰۹-۱۲ ۳۴۵ ۶۷۸۹"))
    }

    @Test
    fun digitsOnlyStripsNonDigits() {
        assertEquals("989123456789", DigitNormalizer.digitsOnly("+98 (912) 345-6789"))
    }
}
