package com.kheyr.sms.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingQrValidatorTest {
    @Test fun acceptsFreshKheyrCode() {
        val content = PairingQrContent("sess-1", "kheyr", expiresAtEpochSeconds = 1000)
        assertEquals(PairingQrResult.Valid("sess-1"), PairingQrValidator.validate(content, nowEpochSeconds = 999))
    }

    @Test fun rejectsExpiredCode() {
        val content = PairingQrContent("sess-1", "kheyr", expiresAtEpochSeconds = 1000)
        assertTrue(PairingQrValidator.validate(content, nowEpochSeconds = 1000) is PairingQrResult.Invalid)
    }

    @Test fun rejectsForeignServer() {
        val content = PairingQrContent("sess-1", "evil", expiresAtEpochSeconds = null)
        assertTrue(PairingQrValidator.validate(content, 0) is PairingQrResult.Invalid)
    }

    @Test fun rejectsMissingSession() {
        assertTrue(PairingQrValidator.validate(PairingQrContent(null, "kheyr", null), 0) is PairingQrResult.Invalid)
        assertTrue(PairingQrValidator.validate(null, 0) is PairingQrResult.Invalid)
    }

    @Test fun acceptsWhenNoExpiryOrServerProvided() {
        assertEquals(PairingQrResult.Valid("s"), PairingQrValidator.validate(PairingQrContent("s", null, null), 12345))
    }
}
