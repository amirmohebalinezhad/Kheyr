package com.kheyr.sms.data

data class BlockedSenderState(val address: String, val blockedAtMillis: Long, val synced: Boolean = false)
