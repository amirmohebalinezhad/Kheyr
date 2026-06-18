package com.kheyr.sms.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object JalaliDateFormatter {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    private val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    private val weekdayNames = mapOf(
        DayOfWeek.SATURDAY to "شنبه",
        DayOfWeek.SUNDAY to "یکشنبه",
        DayOfWeek.MONDAY to "دوشنبه",
        DayOfWeek.TUESDAY to "سه‌شنبه",
        DayOfWeek.WEDNESDAY to "چهارشنبه",
        DayOfWeek.THURSDAY to "پنجشنبه",
        DayOfWeek.FRIDAY to "جمعه",
    )

    fun format(instant: Instant, now: Instant = Instant.now()): String {
        val messageZoned = instant.atZone(zone)
        val nowZoned = now.atZone(zone)
        val messageDate = messageZoned.toLocalDate()
        val today = nowZoned.toLocalDate()
        val time = toPersianDigits(String.format("%02d:%02d", messageZoned.hour, messageZoned.minute))

        return when {
            messageDate == today -> time
            messageDate == today.minusDays(1) -> "دیروز $time"
            isInCurrentWeek(messageDate, today) -> "${weekdayNames[messageDate.dayOfWeek].orEmpty()} $time"
            else -> {
                val jalali = gregorianToJalali(messageDate)
                val datePart = "${toPersianDigits(jalali.day.toString())} ${monthNames[jalali.month - 1]} ${toPersianDigits(jalali.year.toString())}"
                "$datePart $time"
            }
        }
    }

    fun toPersianDigits(text: String): String = buildString(text.length) {
        text.forEach { char ->
            append(if (char in '0'..'9') '۰' + (char - '0') else char)
        }
    }

    private fun isInCurrentWeek(date: LocalDate, today: LocalDate): Boolean {
        val weekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
        val weekEnd = weekStart.plusDays(6)
        return !date.isBefore(weekStart) && !date.isAfter(weekEnd) &&
            ChronoUnit.DAYS.between(date, today) >= 2
    }

    data class JalaliDate(val year: Int, val month: Int, val day: Int)

    fun gregorianToJalali(date: LocalDate): JalaliDate = gregorianToJalali(date.year, date.monthValue, date.dayOfMonth)

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var jy = if (gy > 1600) 979 else 0
        val gy2 = if (gy > 1600) gy - 1600 else gy - 621
        var days = (365 * gy2) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) - 80 + gd + gDaysInMonth[gm - 1]
        jy += 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
        val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30
        return JalaliDate(jy, jm, jd)
    }
}
