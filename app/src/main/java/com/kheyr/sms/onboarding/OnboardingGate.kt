package com.kheyr.sms.onboarding

data class OnboardingGateState(val isDefaultSmsApp: Boolean, val smsPermissionGranted: Boolean, val contactsPermissionGranted: Boolean, val notificationPermissionGranted: Boolean) {
    val canUseFullSmsFeatures: Boolean get() = missingRequirements.isEmpty()
    val missingRequirements: Set<OnboardingRequirement> get() = buildSet {
        if (!isDefaultSmsApp) add(OnboardingRequirement.DefaultSmsRole)
        if (!smsPermissionGranted) add(OnboardingRequirement.SmsPermission)
        if (!contactsPermissionGranted) add(OnboardingRequirement.ContactsPermission)
        if (!notificationPermissionGranted) add(OnboardingRequirement.NotificationPermission)
    }
}
enum class OnboardingRequirement { DefaultSmsRole, SmsPermission, ContactsPermission, NotificationPermission }
