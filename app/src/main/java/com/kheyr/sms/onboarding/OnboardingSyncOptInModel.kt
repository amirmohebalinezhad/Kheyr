package com.kheyr.sms.onboarding

data class OnboardingSyncOptInModel(val offered: Boolean = true, val accepted: Boolean = false, val skipped: Boolean = false)
