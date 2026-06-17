package com.kheyr.sms.conversation

data class SearchableMessage(val id: Long, val body: String, val sender: String)
class ConversationSearchMatcher {
    fun matchingIds(messages: List<SearchableMessage>, query: String): List<Long> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return emptyList()
        return messages.filter { it.body.lowercase().contains(normalized) || it.sender.lowercase().contains(normalized) }.map { it.id }
    }
}
