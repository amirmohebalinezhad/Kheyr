package com.kheyr.sms.ui

import com.kheyr.sms.telephony.SimCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsComposerStateReducerTest {
    private val reducer = SmsComposerStateReducer()

    @Test fun validatesBodyAndSimBeforeSending() {
        assertEquals(ComposerError.EmptyBody, reducer.reduce(SmsComposerState(), SmsComposerEvent.SendRequested).error)
        assertEquals(
            ComposerError.MissingSimSelection,
            reducer.reduce(SmsComposerState(body = "hello", requiresSimSelection = true), SmsComposerEvent.SendRequested).error,
        )
    }

    @Test fun sendRequestStartsSendingWhenValid() {
        val next = reducer.reduce(SmsComposerState(body = "hello", selectedSubscriptionId = 1, requiresSimSelection = true), SmsComposerEvent.SendRequested)

        assertTrue(next.sending)
        assertEquals(null, next.error)
    }

    @Test fun completionClearsBodyAndFailureKeepsBodyRetryable() {
        assertEquals("", reducer.reduce(SmsComposerState(body = "sent", sending = true), SmsComposerEvent.SendCompleted).body)
        val failed = reducer.reduce(SmsComposerState(body = "retry", sending = true), SmsComposerEvent.SendFailed)

        assertFalse(failed.sending)
        assertEquals("retry", failed.body)
        assertEquals(ComposerError.SendFailed, failed.error)
    }

    @Test fun activeSimNumberLabelUsesSelectedSimSlotAndFallsBackToFirstAvailable() {
        val sim1 = SimCard(subscriptionId = 11, slotIndex = 0, displayName = "Personal", carrierName = "A")
        val sim2 = SimCard(subscriptionId = 22, slotIndex = 1, displayName = "Work", carrierName = "B")

        assertEquals("2", activeSimNumberLabel(listOf(sim1, sim2), selectedSubscriptionId = 22))
        assertEquals("1", activeSimNumberLabel(listOf(sim1, sim2), selectedSubscriptionId = 99))
        assertEquals("SIM", activeSimNumberLabel(emptyList(), selectedSubscriptionId = null))
    }
}
