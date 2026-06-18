package com.kheyr.sms.ui

import com.kheyr.sms.data.SmsThread
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ThreadListFilterTest {
    private fun thread(
        displayName: String = "09121234567",
        address: String = "09121234567",
        unreadCount: Int = 0,
    ) = SmsThread(
        id = 1L,
        address = address,
        displayName = displayName,
        lastMessage = "hi",
        lastMessageAt = Instant.now(),
        unreadCount = unreadCount,
    )

    @Test
    fun allMatchesEveryThread() {
        assertTrue(ThreadListFilter.All.matches(thread()))
        assertTrue(ThreadListFilter.All.matches(thread(unreadCount = 3)))
    }

    @Test
    fun unreadMatchesOnlyUnreadThreads() {
        assertTrue(ThreadListFilter.Unread.matches(thread(unreadCount = 2)))
        assertFalse(ThreadListFilter.Unread.matches(thread()))
    }

    @Test
    fun contactsMatchesNamedThreads() {
        assertTrue(ThreadListFilter.Contacts.matches(thread(displayName = "Ali", address = "09121234567")))
        assertFalse(ThreadListFilter.Contacts.matches(thread()))
    }
}
