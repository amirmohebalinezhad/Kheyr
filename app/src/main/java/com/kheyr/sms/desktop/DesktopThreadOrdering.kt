package com.kheyr.sms.desktop

data class DesktopThreadSummary(val threadId: Long, val lastMessageAtMillis: Long, val pinnedAtMillis: Long?)
object DesktopThreadOrdering {
    fun sort(threads: List<DesktopThreadSummary>): List<DesktopThreadSummary> = threads.sortedWith(compareByDescending<DesktopThreadSummary> { it.pinnedAtMillis != null }.thenByDescending { it.pinnedAtMillis ?: it.lastMessageAtMillis }.thenByDescending { it.lastMessageAtMillis })
}
