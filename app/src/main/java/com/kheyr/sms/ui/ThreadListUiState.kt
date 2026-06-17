package com.kheyr.sms.ui

import com.kheyr.sms.domain.ThreadSorter

data class ThreadListUiState(
    val folder: ThreadFolder = ThreadFolder.Inbox,
    val searchQuery: String = "",
    val threads: List<com.kheyr.sms.data.SmsThread> = emptyList(),
) {
    val visibleThreads get() = ThreadSorter.inboxThreads(threads).filter {
        searchQuery.isBlank() || it.displayName.contains(searchQuery, true) || it.address.contains(searchQuery)
    }
}
