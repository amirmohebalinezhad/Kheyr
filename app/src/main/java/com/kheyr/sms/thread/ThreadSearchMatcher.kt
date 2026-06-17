package com.kheyr.sms.thread

data class SearchableThread(val displayName: String, val address: String, val lastMessage: String)
class ThreadSearchMatcher {
    fun matches(thread: SearchableThread, query: String): Boolean {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return true
        return listOf(thread.displayName, thread.address, thread.lastMessage).any { it.lowercase().contains(normalized) }
    }
}
