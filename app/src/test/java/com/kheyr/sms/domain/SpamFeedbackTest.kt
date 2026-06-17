package com.kheyr.sms.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SpamFeedbackTest {
    @Test fun anonymousPayloadExcludesMessageAndThreadIdentifiers() {
        val payload = SpamFeedbackEvent(
            messageId = 42,
            threadId = 99,
            action = SpamFeedbackAction.MarkNotSpam,
            previousClassification = SpamClassification.Spam,
            correctedAt = Instant.EPOCH,
            triggeredRuleIds = listOf("url", "prefix", "url"),
        ).anonymousPayload()

        assertEquals(SpamFeedbackAction.MarkNotSpam, payload.action)
        assertEquals(SpamClassification.Spam, payload.previousClassification)
        assertEquals(listOf("prefix", "url"), payload.triggeredRuleIds)
        assertEquals(false, payload.toString().contains("42"))
        assertEquals(false, payload.toString().contains("99"))
    }
}
