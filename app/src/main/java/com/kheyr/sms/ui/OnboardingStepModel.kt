package com.kheyr.sms.ui

/**
 * Onboarding step model for default SMS, permissions, sync, and spam rules.
 */
data class OnboardingStepModel(
    val id: String,
    val title: String,
    val description: String = "",
    val enabled: Boolean = true,
    val updatedAtMillis: Long = 0L,
) {
    val isVisible: Boolean get() = enabled && title.isNotBlank()
}
