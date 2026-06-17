package com.kheyr.sms.sim

data class ThreadSimPreference(val threadId: Long, val subscriptionId: Int?, val updatedAtMillis: Long) {
    val hasOverride: Boolean get() = subscriptionId != null
}
