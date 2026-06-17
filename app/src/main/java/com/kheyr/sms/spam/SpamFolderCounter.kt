package com.kheyr.sms.spam

data class SpamFolderThreadSummary(val isSpam: Boolean, val unreadCount: Int)
class SpamFolderCounter {
    fun unreadCount(threads: List<SpamFolderThreadSummary>): Int = threads.filter { it.isSpam }.sumOf { it.unreadCount.coerceAtLeast(0) }
}
