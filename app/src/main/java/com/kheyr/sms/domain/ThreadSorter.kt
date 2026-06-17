package com.kheyr.sms.domain

import com.kheyr.sms.data.SmsThread

object ThreadSorter {
    fun inboxThreads(threads: List<SmsThread>): List<SmsThread> = threads
        .filterNot { it.isSpam || it.isArchived }
        .sortedWith(compareByDescending<SmsThread> { it.isPinned }
            .thenByDescending { it.pinnedAt }
            .thenByDescending { it.lastMessageAt })
}
