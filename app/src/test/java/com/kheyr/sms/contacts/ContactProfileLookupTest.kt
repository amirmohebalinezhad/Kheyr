package com.kheyr.sms.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContactProfileLookupTest {
    @Test
    fun matchKeyAlignsIranianPrefixVariants() {
        assertEquals("9123456789", PhoneNumberNormalizer.matchKey("+989123456789"))
        assertEquals("9123456789", PhoneNumberNormalizer.matchKey("09123456789"))
        assertEquals("9123456789", PhoneNumberNormalizer.matchKey("989123456789"))
    }

    @Test
    fun matchKeyAlignsUsPrefixVariants() {
        assertEquals("4155551234", PhoneNumberNormalizer.matchKey("+14155551234"))
        assertEquals("4155551234", PhoneNumberNormalizer.matchKey("14155551234"))
        assertEquals("4155551234", PhoneNumberNormalizer.matchKey("4155551234"))
    }

    @Test
    fun lookupProfileInIndexFindsContactByMatchKey() {
        val profile = ContactProfile(displayName = "Alice", photoUri = null, contactId = 1L)
        val index = mapOf(
            PhoneNumberNormalizer.matchKey("+14155551234") to profile,
        )
        assertNull(lookupProfileInIndex(index, "9999999999"))
        assertNotNull(lookupProfileInIndex(index, "4155551234"))
        assertEquals("Alice", lookupProfileInIndex(index, "+14155551234")?.displayName)
    }

    private fun lookupProfileInIndex(profileIndex: Map<String, ContactProfile>, address: String): ContactProfile? =
        profileIndex[address]
            ?: profileIndex[PhoneNumberNormalizer.normalize(address)]
            ?: profileIndex[PhoneNumberNormalizer.matchKey(address)]
}
