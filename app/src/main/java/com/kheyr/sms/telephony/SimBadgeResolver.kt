package com.kheyr.sms.telephony

object SimBadgeResolver {
    fun badge(subscriptionId: Int?, activeSims: List<SimCard> = emptyList()): String? {
        if (subscriptionId == null) return null
        return activeSims.firstOrNull { it.subscriptionId == subscriptionId }?.displayName
    }
}
