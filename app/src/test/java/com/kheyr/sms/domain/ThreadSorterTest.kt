package com.kheyr.sms.domain

import com.kheyr.sms.data.SmsThread
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ThreadSorterTest {
    @Test fun pinnedThreadsSortByPinDateBeforeNormalThreads() {
        val oldPinned = thread(1, last = "2026-01-03T00:00:00Z", pinned = "2026-01-04T00:00:00Z")
        val newPinned = thread(2, last = "2026-01-01T00:00:00Z", pinned = "2026-01-05T00:00:00Z")
        val recentNormal = thread(3, last = "2026-01-06T00:00:00Z")

        assertEquals(listOf(2L, 1L, 3L), ThreadSorter.inboxThreads(listOf(recentNormal, oldPinned, newPinned)).map { it.id })
    }

    @Test fun spamAndArchivedThreadsAreHiddenFromInbox() {
        val visible = thread(1, last = "2026-01-03T00:00:00Z")
        val spam = thread(2, last = "2026-01-04T00:00:00Z", isSpam = true)
        val archived = thread(3, last = "2026-01-05T00:00:00Z", isArchived = true)

        assertEquals(listOf(1L), ThreadSorter.inboxThreads(listOf(visible, spam, archived)).map { it.id })
    }

    private fun thread(
        id: Long,
        last: String,
        pinned: String? = null,
        isSpam: Boolean = false,
        isArchived: Boolean = false,
    ) = SmsThread(
        id = id,
        address = "+100$id",
        displayName = "Contact $id",
        lastMessage = "Message $id",
        lastMessageAt = Instant.parse(last),
        isPinned = pinned != null,
        pinnedAt = pinned?.let(Instant::parse),
        isSpam = isSpam,
        isArchived = isArchived,
    )
}
