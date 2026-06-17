package com.kheyr.sms.auth

data class LogoutPlan(
    val revokeTokens: Boolean = true,
    val clearSyncKeys: Boolean = true,
    val drainSyncQueue: Boolean = true,
)
