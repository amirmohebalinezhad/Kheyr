package com.kheyr.sms.ui

data class ThreadAvatarPresentation(val initials: String, val hasContactPhoto: Boolean)

object ThreadAvatarMapper {
    fun map(displayName: String, hasPhoto: Boolean = false): ThreadAvatarPresentation = ThreadAvatarPresentation(
        initials = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
        hasContactPhoto = hasPhoto,
    )
}
