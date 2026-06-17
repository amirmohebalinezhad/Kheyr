package com.kheyr.sms.telephony

/** Chooses the subscription used when sending SMS, honoring thread overrides before global defaults. */
class SimRoutingPolicy {
    fun resolve(availableSubscriptions: Set<Int>, preference: SimRoutingPreference): SimRoutingDecision {
        preference.threadSubscriptionId?.let {
            if (it in availableSubscriptions) return SimRoutingDecision.UseSubscription(it, SimRoutingSource.Thread)
        }
        preference.globalSubscriptionId?.let {
            if (it in availableSubscriptions) return SimRoutingDecision.UseSubscription(it, SimRoutingSource.Global)
        }
        return SimRoutingDecision.AskUser
    }
}

data class SimRoutingPreference(val globalSubscriptionId: Int? = null, val threadSubscriptionId: Int? = null)

sealed interface SimRoutingDecision {
    data object AskUser : SimRoutingDecision
    data class UseSubscription(val subscriptionId: Int, val source: SimRoutingSource) : SimRoutingDecision
}

enum class SimRoutingSource { Thread, Global }
