package com.kheyr.sms.notifications

object BlockedSenderPolicy {
    fun shouldSuppress(isBlocked: Boolean, isSpam: Boolean): Boolean = isBlocked || isSpam
}
