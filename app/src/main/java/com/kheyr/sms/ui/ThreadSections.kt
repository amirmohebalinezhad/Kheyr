package com.kheyr.sms.ui

import com.kheyr.sms.data.SmsThread
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class ThreadSection(
    val title: String,
    val threads: List<SmsThread>,
)

/**
 * Groups the already-sorted inbox into sticky date buckets. Pinned conversations keep their own
 * bucket at the top of the inbox; every other folder falls back to plain recency buckets.
 */
object ThreadSectionBuilder {
    fun build(
        threads: List<SmsThread>,
        folder: ThreadFolder,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<ThreadSection> {
        if (threads.isEmpty()) return emptyList()
        if (folder == ThreadFolder.Pinned) {
            return listOf(ThreadSection("Pinned", threads))
        }

        val sections = LinkedHashMap<String, MutableList<SmsThread>>()
        val pinned = if (folder == ThreadFolder.Inbox) threads.filter { it.isPinned } else emptyList()
        if (pinned.isNotEmpty()) sections["Pinned"] = pinned.toMutableList()

        val today = now.atZone(zone).toLocalDate()
        threads.filterNot { folder == ThreadFolder.Inbox && it.isPinned }.forEach { thread ->
            val date = thread.lastMessageAt.atZone(zone).toLocalDate()
            val daysAgo = ChronoUnit.DAYS.between(date, today)
            val label = when {
                daysAgo <= 0L -> "Today"
                daysAgo == 1L -> "Yesterday"
                daysAgo in 2L..6L -> "This week"
                else -> "Older"
            }
            sections.getOrPut(label) { mutableListOf() }.add(thread)
        }
        return sections.map { (title, items) -> ThreadSection(title, items) }
    }
}
