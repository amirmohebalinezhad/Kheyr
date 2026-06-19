package com.kheyr.sms.telephony

/** Resolves which subscription to use in the message composer for a thread. */
object ComposerSimResolver {
    private val policy = SimRoutingPolicy()

    fun resolve(
        sims: List<SimCard>,
        threadSubscriptionId: Int?,
        globalSubscriptionId: Int?,
    ): Int? {
        if (sims.isEmpty()) return null
        val available = sims.map { it.subscriptionId }.toSet()
        val threadSub = normalizeThreadSubscription(sims, threadSubscriptionId)
        return when (val decision = policy.resolve(available, SimRoutingPreference(globalSubscriptionId, threadSub))) {
            is SimRoutingDecision.UseSubscription -> decision.subscriptionId
            SimRoutingDecision.AskUser -> sims.first().subscriptionId
        }
    }

    private fun normalizeThreadSubscription(sims: List<SimCard>, threadSubscriptionId: Int?): Int? {
        if (threadSubscriptionId == null) return null
        val available = sims.map { it.subscriptionId }.toSet()
        if (threadSubscriptionId in available) return threadSubscriptionId
        // A stored subscriptionId that is no longer present (e.g. after a SIM swap) must not be
        // reinterpreted as a slot index: a stale subscriptionId can coincidentally match a valid
        // slotIndex and silently route the reply out the wrong SIM. Return null so the caller falls
        // back to global/default routing.
        return null
    }
}
