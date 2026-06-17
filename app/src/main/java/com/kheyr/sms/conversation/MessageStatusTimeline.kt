package com.kheyr.sms.conversation

enum class SmsDeliveryState { Sending, Sent, Delivered, Failed }
class MessageStatusTimeline {
    fun canTransition(from: SmsDeliveryState, to: SmsDeliveryState): Boolean = when (from) {
        SmsDeliveryState.Sending -> to in setOf(SmsDeliveryState.Sent, SmsDeliveryState.Failed)
        SmsDeliveryState.Sent -> to in setOf(SmsDeliveryState.Delivered, SmsDeliveryState.Failed)
        SmsDeliveryState.Delivered, SmsDeliveryState.Failed -> false
    }
}
