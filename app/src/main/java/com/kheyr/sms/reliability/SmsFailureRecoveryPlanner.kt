package com.kheyr.sms.reliability

enum class SmsRecoveryAction { Retry, SwitchSim, ShowGuidance }

object SmsFailureRecoveryPlanner {
    fun action(failureCode: String, dualSimAvailable: Boolean): SmsRecoveryAction = when {
        failureCode.contains("radio", true) && dualSimAvailable -> SmsRecoveryAction.SwitchSim
        failureCode.contains("generic", true) -> SmsRecoveryAction.Retry
        else -> SmsRecoveryAction.ShowGuidance
    }
}
