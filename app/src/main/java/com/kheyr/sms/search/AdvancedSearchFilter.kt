package com.kheyr.sms.search

data class AdvancedSearchFilter(val senderQuery: String? = null, val bodyQuery: String? = null, val fromMillis: Long? = null, val toMillis: Long? = null) {
    fun matches(sender: String, body: String, timestampMillis: Long): Boolean =
        (senderQuery.isNullOrBlank() || sender.contains(senderQuery, ignoreCase = true)) &&
            (bodyQuery.isNullOrBlank() || body.contains(bodyQuery, ignoreCase = true)) &&
            (fromMillis == null || timestampMillis >= fromMillis) &&
            (toMillis == null || timestampMillis <= toMillis)
}
