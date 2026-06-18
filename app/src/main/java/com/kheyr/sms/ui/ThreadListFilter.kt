package com.kheyr.sms.ui

import com.kheyr.sms.data.SmsThread

enum class ThreadListFilter {
    All,
    Unread,
    Contacts,
}

fun ThreadListFilter.matches(thread: SmsThread): Boolean = when (this) {
    ThreadListFilter.All -> true
    ThreadListFilter.Unread -> thread.unreadCount > 0
    ThreadListFilter.Contacts -> thread.displayName.isNotBlank() && thread.displayName != thread.address
}
