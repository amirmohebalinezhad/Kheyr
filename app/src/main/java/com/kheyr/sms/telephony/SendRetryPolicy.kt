package com.kheyr.sms.telephony

import com.kheyr.sms.data.MessageStatus

class SendRetryPolicy(private val maxAttempts: Int = 3) {
    init { require(maxAttempts > 0) { "maxAttempts must be positive" } }

    fun evaluate(status: MessageStatus, attempts: Int): SendRetryDecision = when {
        status != MessageStatus.Failed -> SendRetryDecision.NotRetryable
        attempts >= maxAttempts -> SendRetryDecision.GiveUp
        else -> SendRetryDecision.Retry(attempts + 1)
    }
}

sealed interface SendRetryDecision {
    data object NotRetryable : SendRetryDecision
    data object GiveUp : SendRetryDecision
    data class Retry(val nextAttempt: Int) : SendRetryDecision
}
