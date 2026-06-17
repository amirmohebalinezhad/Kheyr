package com.kheyr.sms.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
}
