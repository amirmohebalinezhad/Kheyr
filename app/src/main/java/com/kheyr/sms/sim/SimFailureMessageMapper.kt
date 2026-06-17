package com.kheyr.sms.sim

enum class SimSendFailure { MissingSelection, SubscriptionUnavailable, RadioOff, CarrierRejected }
object SimFailureMessageMapper {
    fun message(failure: SimSendFailure): String = when (failure) {
        SimSendFailure.MissingSelection -> "Choose a SIM before sending."
        SimSendFailure.SubscriptionUnavailable -> "The selected SIM is not available."
        SimSendFailure.RadioOff -> "Mobile radio is off for the selected SIM."
        SimSendFailure.CarrierRejected -> "The carrier rejected this SMS."
    }
}
