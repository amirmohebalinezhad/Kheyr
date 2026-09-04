package com.kheyr.sms.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliDateFormatterTest {
    private val zone = ZoneId.systemDefault()

    @Test
    fun todayShowsOnlyTime() {
        val now = Instant.now()
        val formatted = JalaliDateFormatter.format(now, now)
        assertFalse(formatted.contains("دیروز"))
        assertTrue(formatted.contains(":"))
    }

    @Test
    fun yesterdayShowsLabelAndTime() {
        val now = Instant.now()
        val yesterday = now.atZone(zone).toLocalDate().minusDays(1).atTime(9, 5).atZone(zone).toInstant()
        assertTrue(JalaliDateFormatter.format(yesterday, now).startsWith("دیروز"))
    }

    @Test
    fun olderDateShowsJalaliDateAndTime() {
        val now = Instant.now()
        val older = now.atZone(zone).toLocalDate().minusMonths(2).atTime(8, 15).atZone(zone).toInstant()
        val formatted = JalaliDateFormatter.format(older, now)
        assertTrue(formatted.contains("۰۸:۱۵"))
    }

    @Test
    fun gregorianToJalaliConvertsNowruz2026() {
        val jalali = JalaliDateFormatter.gregorianToJalali(LocalDate.of(2026, 3, 21))
        assertEquals(1405, jalali.year)
        assertEquals(1, jalali.month)
        assertEquals(1, jalali.day)
    }

    @Test
    fun gregorianToJalaliConvertsNowruzInNonLeapYears() {
        assertJalali(LocalDate.of(2023, 3, 21), 1402, 1, 1)
        assertJalali(LocalDate.of(2025, 3, 21), 1404, 1, 1)
    }

    // Regression for the dropped leap-day correction: every date from 1 March to 31 December of a
    // Gregorian leap year used to convert to the previous Jalali day.
    @Test
    fun gregorianToJalaliHandlesLeapYearAfterFebruary() {
        assertJalali(LocalDate.of(2024, 3, 1), 1402, 12, 11)
        assertJalali(LocalDate.of(2024, 3, 20), 1403, 1, 1)
        assertJalali(LocalDate.of(2024, 12, 31), 1403, 10, 11)
    }

    @Test
    fun gregorianToJalaliKeepsLeapYearBeforeMarchCorrect() {
        assertJalali(LocalDate.of(2024, 1, 15), 1402, 10, 25)
        assertJalali(LocalDate.of(2024, 2, 29), 1402, 12, 10)
    }

    @Test
    fun timeDigitsArePersianUnderArabicIndicDefaultLocale() {
        val original = Locale.getDefault()
        try {
            // A locale whose default numbering system is Arabic-Indic: String.format would emit
            // non-ASCII digits that toPersianDigits cannot map.
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            val now = Instant.now()
            val older = now.atZone(zone).toLocalDate().minusMonths(2).atTime(8, 15).atZone(zone).toInstant()
            val formatted = JalaliDateFormatter.format(older, now)
            assertTrue(formatted.contains("۰۸:۱۵"))
            assertFalse(formatted.any { it in '٠'..'٩' })
        } finally {
            Locale.setDefault(original)
        }
    }

    private fun assertJalali(date: LocalDate, year: Int, month: Int, day: Int) {
        val jalali = JalaliDateFormatter.gregorianToJalali(date)
        val actual = "${jalali.year}-${jalali.month}-${jalali.day}"
        assertEquals(date.toString(), "$year-$month-$day", actual)
    }
}
