package com.kheyr.sms.telephony

import org.junit.Assert.assertEquals
import org.junit.Test

class SimRoutingPolicyTest {
    private val policy = SimRoutingPolicy()

    @Test fun threadDefaultOverridesGlobalDefault() {
        assertEquals(
            SimRoutingDecision.UseSubscription(2, SimRoutingSource.Thread),
            policy.resolve(setOf(1, 2), SimRoutingPreference(globalSubscriptionId = 1, threadSubscriptionId = 2)),
        )
    }

    @Test fun globalDefaultIsUsedWhenThreadDefaultUnavailable() {
        assertEquals(
            SimRoutingDecision.UseSubscription(1, SimRoutingSource.Global),
            policy.resolve(setOf(1), SimRoutingPreference(globalSubscriptionId = 1, threadSubscriptionId = 2)),
        )
    }

    @Test fun asksUserWhenNoPreferredSubscriptionIsAvailable() {
        assertEquals(SimRoutingDecision.AskUser, policy.resolve(setOf(3), SimRoutingPreference(globalSubscriptionId = 1)))
    }
}
