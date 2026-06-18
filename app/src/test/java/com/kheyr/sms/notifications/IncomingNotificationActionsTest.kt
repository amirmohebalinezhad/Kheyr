package com.kheyr.sms.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingNotificationActionsTest {
    @Test fun exposesCopyableOtpCode() {
        assertEquals("123456", IncomingNotificationActions.copyableCode("Your verification code is 123456"))
    }

    @Test fun noCopyableCodeForPlainText() {
        assertNull(IncomingNotificationActions.copyableCode("Hey, are we still on for lunch?"))
    }

    @Test fun sanitizeReplyTrimsWhitespace() {
        assertEquals("on my way", IncomingNotificationActions.sanitizeReply("  on my way  "))
    }

    @Test fun sanitizeReplyRejectsBlankInput() {
        assertNull(IncomingNotificationActions.sanitizeReply("   "))
        assertNull(IncomingNotificationActions.sanitizeReply(null))
        assertNull(IncomingNotificationActions.sanitizeReply(""))
    }
}
