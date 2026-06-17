package com.kheyr.sms.onboarding

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingGateTest {
    @Test fun fullAccessRequiresEveryOnboardingRequirement() {
        val partial = OnboardingGateState(
            isDefaultSmsApp = true,
            smsPermissionGranted = true,
            contactsPermissionGranted = false,
            notificationPermissionGranted = false,
        )

        assertFalse(partial.canUseFullSmsFeatures)
        assertTrue(partial.missingRequirements.isNotEmpty())
    }

    @Test fun fullAccessGrantedWhenAllRequirementsMet() {
        val ready = OnboardingGateState(true, true, true, true)

        assertTrue(ready.canUseFullSmsFeatures)
        assertTrue(ready.missingRequirements.isEmpty())
    }
}
