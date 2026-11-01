package com.kheyr.sms.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PersianTextNormalizerTest {
    private val zwnj = Char(0x200C).toString()

    @Test fun foldsArabicYehOntoFarsiYeh() {
        assertEquals("تایید", PersianTextNormalizer.normalize("تاييد"))
    }

    @Test fun foldsArabicKafOntoKeheh() {
        assertEquals("کد", PersianTextNormalizer.normalize("كد"))
    }

    @Test fun stripsZeroWidthNonJoiner() {
        // The rules spell these compounds with an optional space, so the ZWNJ has to go entirely.
        assertEquals("فعالسازی", PersianTextNormalizer.normalize("فعال" + zwnj + "سازی"))
    }

    @Test fun foldsPersianAndArabicIndicDigits() {
        assertEquals("123456", PersianTextNormalizer.normalize("۱۲۳۴۵۶"))
        assertEquals("123456", PersianTextNormalizer.normalize("١٢٣٤٥٦"))
    }

    @Test fun leavesAlreadyNormalisedTextUnchanged() {
        val normalized = "کد تایید: 123456"
        assertEquals(normalized, PersianTextNormalizer.normalize(normalized))
    }

    @Test fun isIdempotent() {
        val raw = "كد تاييد فعال" + zwnj + "سازی: ۵۱۷۰۷"
        val once = PersianTextNormalizer.normalize(raw)
        assertEquals(once, PersianTextNormalizer.normalize(once))
    }

    @Test fun preservesEverythingElse() {
        // Latin, punctuation, emoji and the opt-out footer must survive untouched.
        val body = "Order OK! example.ir ☎ 02112345678 لغو11"
        assertEquals(body, PersianTextNormalizer.normalize(body))
    }

    @Test fun handlesEmptyInput() {
        assertEquals("", PersianTextNormalizer.normalize(""))
    }
}
