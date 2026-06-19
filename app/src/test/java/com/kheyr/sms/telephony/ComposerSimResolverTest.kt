package com.kheyr.sms.telephony

import org.junit.Assert.assertEquals
import org.junit.Test

class ComposerSimResolverTest {
    private val sim1 = SimCard(subscriptionId = 11, slotIndex = 0, displayName = "SIM 1", carrierName = "A")
    private val sim2 = SimCard(subscriptionId = 22, slotIndex = 1, displayName = "SIM 2", carrierName = "B")

    @Test
    fun prefersThreadSubscriptionId() {
        assertEquals(22, ComposerSimResolver.resolve(listOf(sim1, sim2), threadSubscriptionId = 22, globalSubscriptionId = 11))
    }

    @Test
    fun doesNotReinterpretStaleSubscriptionIdAsSlotIndex() {
        // 1 is not a current subscriptionId; it must NOT be treated as slotIndex 1 (sim2),
        // which would silently misroute the SIM. It falls back to the global default instead.
        assertEquals(11, ComposerSimResolver.resolve(listOf(sim1, sim2), threadSubscriptionId = 1, globalSubscriptionId = 11))
    }

    @Test
    fun fallsBackToGlobalDefaultWhenThreadUnavailable() {
        assertEquals(11, ComposerSimResolver.resolve(listOf(sim1, sim2), threadSubscriptionId = 99, globalSubscriptionId = 11))
    }

    @Test
    fun fallsBackToFirstSimWhenNoPreferenceMatches() {
        assertEquals(11, ComposerSimResolver.resolve(listOf(sim1, sim2), threadSubscriptionId = null, globalSubscriptionId = 99))
    }

    @Test
    fun returnsNullWhenNoSimsAvailable() {
        assertEquals(null, ComposerSimResolver.resolve(emptyList(), threadSubscriptionId = 11, globalSubscriptionId = 11))
    }
}
