package com.kheyr.sms.ui

import com.kheyr.sms.telephony.SimCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationHeaderMapperTest {
    private val mapper = ConversationHeaderMapper()

    @Test fun headerShowsContactSimAndActions() {
        val sims = listOf(SimCard(subscriptionId = 0, slotIndex = 0, displayName = "SIM 1", carrierName = "Carrier"))
        val header = mapper.map(ConversationHeaderInput("+15551234567", "Alice", subscriptionId = 0, messageCount = 3), sims)

        assertEquals("Alice", header.title)
        assertEquals("SIM 1", header.subtitle)
        assertTrue(header.callEnabled)
        assertTrue(header.infoEnabled)
        assertTrue(header.searchEnabled)
    }

    @Test fun searchDisabledWithoutMessagesAndTitleFallsBackToAddress() {
        val header = mapper.map(ConversationHeaderInput("SERVICE", "", subscriptionId = null, messageCount = 0))

        assertEquals("SERVICE", header.title)
        assertFalse(header.callEnabled)
        assertFalse(header.searchEnabled)
    }
}
