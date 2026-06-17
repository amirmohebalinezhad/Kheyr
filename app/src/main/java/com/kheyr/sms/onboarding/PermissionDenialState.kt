package com.kheyr.sms.onboarding

enum class PermissionDenialMode { LimitedInbox, NoNotifications, Blocked }

data class PermissionDenialState(val smsDenied: Boolean, val contactsDenied: Boolean, val notificationsDenied: Boolean) {
    val mode: PermissionDenialMode = when {
        smsDenied -> PermissionDenialMode.Blocked
        notificationsDenied -> PermissionDenialMode.NoNotifications
        else -> PermissionDenialMode.LimitedInbox
    }
}
