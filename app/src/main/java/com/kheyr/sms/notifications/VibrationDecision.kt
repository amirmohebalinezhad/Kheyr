package com.kheyr.sms.notifications

data class VibrationInputs(val globalEnabled: Boolean, val threadMuted: Boolean, val silentUnknownSender: Boolean)
object VibrationDecisionResolver {
    fun shouldVibrate(inputs: VibrationInputs): Boolean = inputs.globalEnabled && !inputs.threadMuted && !inputs.silentUnknownSender
}
