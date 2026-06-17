package com.kheyr.sms.telephony

import com.kheyr.sms.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SendRetryPolicyTest {
    private val policy = SendRetryPolicy(maxAttempts = 3)

    @Test fun failedMessagesCanRetryUntilLimit() {
        assertEquals(SendRetryDecision.Retry(2), policy.evaluate(MessageStatus.Failed, attempts = 1))
    }

    @Test fun failedMessagesGiveUpAtLimit() {
        assertEquals(SendRetryDecision.GiveUp, policy.evaluate(MessageStatus.Failed, attempts = 3))
    }

    @Test fun nonFailedMessagesAreNotRetryable() {
        assertEquals(SendRetryDecision.NotRetryable, policy.evaluate(MessageStatus.Sent, attempts = 1))
    }
}
