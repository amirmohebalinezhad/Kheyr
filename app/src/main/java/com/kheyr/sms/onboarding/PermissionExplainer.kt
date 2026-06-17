package com.kheyr.sms.onboarding

object PermissionExplainer {
    fun reason(permission: OnboardingRequirement): String = when (permission) {
        OnboardingRequirement.DefaultSmsRole -> "Required to read, send, receive, and manage SMS conversations."
        OnboardingRequirement.SmsPermission -> "Allows importing existing SMS and handling new messages."
        OnboardingRequirement.ContactsPermission -> "Shows contact names and applies contact-specific notification rules."
        OnboardingRequirement.NotificationPermission -> "Allows message alerts after spam and privacy policies are applied."
    }
}
