package com.kheyr.sms.onboarding

data class DefaultRoleMonitorState(val wasDefaultSmsApp: Boolean, val isDefaultSmsApp: Boolean) {
    val shouldWarnRoleRemoved: Boolean get() = wasDefaultSmsApp && !isDefaultSmsApp
}
