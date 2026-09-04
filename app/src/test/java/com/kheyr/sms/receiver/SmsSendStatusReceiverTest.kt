package com.kheyr.sms.receiver

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsSendStatusReceiverTest {
    @Test fun sentCallbackTreatsResultOkAsSuccess() {
        assertTrue(SmsSendStatusDecider.sentSucceeded(Activity.RESULT_OK))
    }

    @Test fun sentCallbackTreatsGenericFailureAsFailure() {
        assertFalse(SmsSendStatusDecider.sentSucceeded(android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE))
    }

    @Test fun deliveryReportBelowPendingRangeIsDelivered() {
        assertEquals(DeliveryOutcome.Delivered, SmsSendStatusDecider.deliveryOutcome(0x00))
        assertEquals(DeliveryOutcome.Delivered, SmsSendStatusDecider.deliveryOutcome(0x1F))
    }

    @Test fun deliveryReportInTemporaryRangeLeavesTheMessageAlone() {
        assertEquals(DeliveryOutcome.Pending, SmsSendStatusDecider.deliveryOutcome(0x20))
        assertEquals(DeliveryOutcome.Pending, SmsSendStatusDecider.deliveryOutcome(0x3F))
    }

    @Test fun negativeDeliveryReportIsAFailure() {
        assertEquals(DeliveryOutcome.Failed, SmsSendStatusDecider.deliveryOutcome(0x40))
        assertEquals(DeliveryOutcome.Failed, SmsSendStatusDecider.deliveryOutcome(0x60))
    }

    @Test fun missingOrUnparseableReportFallsBackToTheResultCode() {
        assertEquals(DeliveryOutcome.Delivered, SmsSendStatusDecider.deliveryOutcome(null))
    }
}
