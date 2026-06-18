package com.kheyr.sms.receiver

import android.app.Activity
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
}
