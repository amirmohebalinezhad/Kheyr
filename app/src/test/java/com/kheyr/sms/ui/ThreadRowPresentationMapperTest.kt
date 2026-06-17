package com.kheyr.sms.ui

import com.kheyr.sms.data.SmsThread
import com.kheyr.sms.telephony.SimCard
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadRowPresentationMapperTest {
    private val mapper = ThreadRowPresentationMapper()

    @Test fun mapsRequiredThreadRowBadges() {
        val sims = listOf(SimCard(subscriptionId = 1, slotIndex = 1, displayName = "SIM 2", carrierName = "Carrier"))
        val row = mapper.map(thread(unreadCount = 120, isPinned = true, isMuted = true, isSpam = true, simSlot = 1), ThreadFolder.Spam, sims)

        assertEquals("Alice", row.title)
        assertEquals("99+", row.unreadBadge)
        assertEquals("SIM 2", row.simBadge)
        assertTrue(row.showPinned)
        assertTrue(row.showMuted)
        assertTrue(row.showSpamBadge)
    }

    @Test fun spamBadgeOnlyShowsInsideSpamFolder() {
        assertFalse(mapper.map(thread(isSpam = true), ThreadFolder.Inbox).showSpamBadge)
    }

    private fun thread(
        unreadCount: Int = 0,
        isPinned: Boolean = false,
        isMuted: Boolean = false,
        isSpam: Boolean = false,
        simSlot: Int? = null,
    ) = SmsThread(
        id = 1,
        address = "+15551234567",
        displayName = "Alice",
        lastMessage = "hello",
        lastMessageAt = Instant.EPOCH,
        unreadCount = unreadCount,
        isPinned = isPinned,
        isMuted = isMuted,
        isSpam = isSpam,
        simSlot = simSlot,
    )
}
