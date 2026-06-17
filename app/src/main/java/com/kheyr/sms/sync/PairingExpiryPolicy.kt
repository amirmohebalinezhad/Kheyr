package com.kheyr.sms.sync

import java.time.Duration
import java.time.Instant
class PairingExpiryPolicy(private val ttl: Duration = Duration.ofMinutes(5)) {
    fun expiresAt(createdAt: Instant): Instant = createdAt.plus(ttl)
    fun isExpired(createdAt: Instant, now: Instant): Boolean = !expiresAt(createdAt).isAfter(now)
}
