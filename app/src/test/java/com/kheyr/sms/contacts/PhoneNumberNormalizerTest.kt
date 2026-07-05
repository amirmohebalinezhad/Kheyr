package com.kheyr.sms.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberNormalizerTest {
    @Test
    fun alphanumericSenderIdIsKeptIntactNotStrippedToBlank() {
        // Regression: digit-stripping collapsed every alphanumeric sender ID to "".
        assertEquals("VERIFY", PhoneNumberNormalizer.normalize("VERIFY"))
        assertEquals("Chase", PhoneNumberNormalizer.normalize("Chase"))
        assertEquals("VERIFY", PhoneNumberNormalizer.matchKey("VERIFY"))
        assertEquals("CHASE", PhoneNumberNormalizer.matchKey("Chase"))
    }

    @Test
    fun distinctAlphanumericSendersDoNotCollide() {
        // The core bug: "CHASE" and "VERIFY" both normalized to "" and matched each other,
        // merging/mislabeling unrelated conversations.
        assertFalse(PhoneNumberNormalizer.matches("CHASE", "VERIFY"))
        assertTrue(PhoneNumberNormalizer.matches("VERIFY", "verify"))
        assertTrue(PhoneNumberNormalizer.matches("VERIFY", "VERIFY"))
    }

    @Test
    fun alphanumericSenderNeverMatchesAPhoneNumber() {
        assertFalse(PhoneNumberNormalizer.matches("VERIFY", "+14155551234"))
        assertFalse(PhoneNumberNormalizer.matches("+14155551234", "VERIFY"))
    }

    @Test
    fun blankAddressesNeverMatch() {
        assertFalse(PhoneNumberNormalizer.matches("", ""))
        assertFalse(PhoneNumberNormalizer.matches("   ", ""))
    }

    @Test
    fun phoneNumberMatchingIsUnchanged() {
        assertTrue(PhoneNumberNormalizer.matches("+14155551234", "14155551234"))
        assertTrue(PhoneNumberNormalizer.matches("09123456789", "+989123456789"))
        assertFalse(PhoneNumberNormalizer.matches("+14155551234", "+14155559999"))
    }

    @Test
    fun isAlphanumericSenderDetectsLetters() {
        assertTrue(PhoneNumberNormalizer.isAlphanumericSender("VERIFY"))
        assertTrue(PhoneNumberNormalizer.isAlphanumericSender("ICICIB"))
        assertFalse(PhoneNumberNormalizer.isAlphanumericSender("+14155551234"))
        assertFalse(PhoneNumberNormalizer.isAlphanumericSender("0912-345-6789"))
    }
}
