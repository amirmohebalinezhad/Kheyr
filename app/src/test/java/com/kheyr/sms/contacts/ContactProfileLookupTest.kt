package com.kheyr.sms.contacts

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ContactProfileLookupTest {
    @After fun tearDown() {
        // The contact cache is process-wide state; leaving it warm would leak into other tests.
        ContactRepository(ApplicationProvider.getApplicationContext<Context>()).invalidateCache()
    }

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

    @Test
    fun lookupProfileInIndexIgnoresBlankKeyedEntries() {
        val repository = ContactRepository(ApplicationProvider.getApplicationContext())
        val profile = ContactProfile(displayName = "Alice", photoUri = null, contactId = 1L)
        // An alphanumeric sender normalizes to a blank key; it must not collide with a blank-keyed entry.
        val index = mapOf("" to profile)
        assertNull(repository.lookupProfileInIndex(index, "VERIFY"))
    }

    @Test
    fun contactCacheIsSharedAcrossInstancesAndClearedByInvalidate() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.READ_CONTACTS)
        ContactRepository(context).invalidateCache()
        assertFalse(ContactRepository.isContactCacheWarm())

        ContactRepository(context).lookupProfile("+14155551234")
        // A second, independently constructed repository sees the cache the first one built.
        assertTrue(ContactRepository.isContactCacheWarm())

        ContactRepository(context).invalidateCache()
        assertFalse(ContactRepository.isContactCacheWarm())
    }

    private fun lookupProfileInIndex(profileIndex: Map<String, ContactProfile>, address: String): ContactProfile? =
        profileIndex[address]
            ?: profileIndex[PhoneNumberNormalizer.normalize(address)]
            ?: profileIndex[PhoneNumberNormalizer.matchKey(address)]
}
