package com.kheyr.sms.ui

import com.kheyr.sms.data.MessageDirection
import com.kheyr.sms.data.MessageStatus
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationTimelineTest {
    private val zone = ZoneId.of("UTC")
    private val now = Instant.parse("2026-01-10T12:00:00Z")

    private fun row(id: Long, at: String) = ConversationMessageRow(
        id = id,
        body = "Message $id",
        layout = ConversationBubbleLayout(
            alignment = BubbleAlignment.Start,
            showBubble = true,
            emphasizeEmoji = false,
        ),
        timeLabel = "12:00",
        showRetry = false,
        copyableCode = null,
        timestamp = Instant.parse(at),
        direction = MessageDirection.Incoming,
        status = MessageStatus.Received,
    )

    @Test
    fun dayLabelsCoverTodayYesterdayAndWeekday() {
        assertEquals("Today", ConversationTimeline.dayLabel(Instant.parse("2026-01-10T01:00:00Z"), now, zone))
        assertEquals("Yesterday", ConversationTimeline.dayLabel(Instant.parse("2026-01-09T23:00:00Z"), now, zone))
        assertEquals("Wednesday", ConversationTimeline.dayLabel(Instant.parse("2026-01-07T10:00:00Z"), now, zone))
    }

    @Test
    fun olderMessagesUseJalaliMonthNames() {
        val label = ConversationTimeline.dayLabel(Instant.parse("2025-12-20T10:00:00Z"), now, zone)

        assertTrue(label.contains("Azar"))
        assertTrue(label.contains("29"))
    }

    @Test
    fun separatorIsInsertedOnlyWhenTheDayChanges() {
        val items = ConversationTimeline.build(
            listOf(
                row(1, "2026-01-10T08:00:00Z"),
                row(2, "2026-01-10T09:00:00Z"),
                row(3, "2026-01-09T22:00:00Z"),
            ),
            now,
            zone,
        )

        assertEquals(5, items.size)
        assertEquals("Today", (items[0] as ConversationTimelineItem.Day).label)
        assertTrue(items[1] is ConversationTimelineItem.Message)
        assertTrue(items[2] is ConversationTimelineItem.Message)
        assertEquals("Yesterday", (items[3] as ConversationTimelineItem.Day).label)
        assertEquals(3L, (items[4] as ConversationTimelineItem.Message).row.id)
    }
}
