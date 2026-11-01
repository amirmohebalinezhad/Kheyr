package com.kheyr.sms.ui

import com.kheyr.sms.util.JalaliDateFormatter
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

sealed interface ConversationTimelineItem {
    data class Day(val label: String) : ConversationTimelineItem
    data class Message(val row: ConversationMessageRow) : ConversationTimelineItem
}

/**
 * Inserts day separators into a conversation without changing message order. Kept pure so the
 * grouping can be unit-tested independently of Compose.
 */
object ConversationTimeline {
    private val monthNames = listOf(
        "Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar",
        "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand",
    )

    fun build(
        rows: List<ConversationMessageRow>,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<ConversationTimelineItem> {
        val items = ArrayList<ConversationTimelineItem>(rows.size + 4)
        var lastDate: LocalDate? = null
        rows.forEach { row ->
            val date = row.timestamp.atZone(zone).toLocalDate()
            if (date != lastDate) {
                items += ConversationTimelineItem.Day(dayLabel(row.timestamp, now, zone))
                lastDate = date
            }
            items += ConversationTimelineItem.Message(row)
        }
        return items
    }

    fun dayLabel(
        instant: Instant,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val date = instant.atZone(zone).toLocalDate()
        val today = now.atZone(zone).toLocalDate()
        val daysAgo = ChronoUnit.DAYS.between(date, today)
        return when {
            daysAgo == 0L -> "Today"
            daysAgo == 1L -> "Yesterday"
            daysAgo in 2L..6L -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            else -> {
                val jalali = JalaliDateFormatter.gregorianToJalali(date)
                val currentJalali = JalaliDateFormatter.gregorianToJalali(today)
                val month = monthNames.getOrElse(jalali.month - 1) { "" }
                if (jalali.year == currentJalali.year) {
                    "${jalali.day} $month"
                } else {
                    "${jalali.day} $month ${jalali.year}"
                }
            }
        }
    }
}
