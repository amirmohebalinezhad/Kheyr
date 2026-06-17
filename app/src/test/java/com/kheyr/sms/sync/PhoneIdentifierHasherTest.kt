package com.kheyr.sms.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PhoneIdentifierHasherTest {
    @Test fun hashesNormalizeFormattingAndHideRawPhoneNumber() {
        val hasher = PhoneIdentifierHasher("user-salt")
        val formatted = hasher.hash("+1 (555) 123-4567")
        val normalized = hasher.hash("+15551234567")

        assertEquals(normalized, formatted)
        assertFalse(formatted.contains("555"))
    }

    @Test fun differentSaltsProduceDifferentHashes() {
        assertNotEquals(PhoneIdentifierHasher("a").hash("+15551234567"), PhoneIdentifierHasher("b").hash("+15551234567"))
    }
}
