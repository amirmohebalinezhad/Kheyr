package com.kheyr.sms.ui

import com.kheyr.sms.data.SmsThread
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadSectionsTest {
    private val zone = ZoneId.of("UTC")
    private val now = Instant.parse("2026-01-10T12:00:00Z")

    private fun thread(id: Long, at: String, pinned: Boolean = false) = SmsThread(
        id = id,
        address = "+1555000$id",
        displayName = "Person $id",
        lastMessage = "Hello",
        lastMessageAt = Instant.parse(at),
        isPinned = pinned,
    )

    @Test
    fun pinnedConversationsGetTheirOwnTopSection() {
        val sections = ThreadSectionBuilder.build(
            listOf(
                thread(1, "2026-01-10T09:00:00Z", pinned = true),
                thread(2, "2026-01-10T08:00:00Z"),
                thread(3, "2026-01-09T08:00:00Z"),
                thread(4, "2025-12-20T08:00:00Z"),
            ),
            ThreadFolder.Inbox,
            now,
            zone,
        )

        assertEquals(listOf("Pinned", "Today", "Yesterday", "Older"), sections.map { it.title })
        assertEquals(listOf(1L), sections.first().threads.map { it.id })
        assertEquals(listOf(2L), sections[1].threads.map { it.id })
    }

    @Test
    fun thisWeekBucketCoversTwoToSixDays() {
        val sections = ThreadSectionBuilder.build(
            listOf(thread(1, "2026-01-06T08:00:00Z")),
            ThreadFolder.Archived,
            now,
            zone,
        )

        assertEquals(listOf("This week"), sections.map { it.title })
    }

    @Test
    fun pinnedFolderStaysASingleBucket() {
        val sections = ThreadSectionBuilder.build(
            listOf(
                thread(1, "2026-01-10T09:00:00Z", pinned = true),
                thread(2, "2025-12-01T09:00:00Z", pinned = true),
            ),
            ThreadFolder.Pinned,
            now,
            zone,
        )

        assertEquals(listOf("Pinned"), sections.map { it.title })
        assertEquals(2, sections.single().threads.size)
    }

    @Test
    fun emptyInboxHasNoSections() {
        assertEquals(emptyList<ThreadSection>(), ThreadSectionBuilder.build(emptyList(), ThreadFolder.Inbox, now, zone))
    }
}
