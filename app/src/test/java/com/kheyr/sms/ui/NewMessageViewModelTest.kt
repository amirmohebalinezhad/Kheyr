package com.kheyr.sms.ui

import com.kheyr.sms.contacts.DeviceContact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NewMessageViewModelTest {
    private val contacts = listOf(
        DeviceContact(id = 1, displayName = "Alice Adams", phoneNumber = "+1 (555) 123-4567"),
        DeviceContact(id = 2, displayName = "Bob Baker", phoneNumber = "555-765-4321"),
    )

    @Test
    fun filtersContactsByNameAndPhoneNumber() {
        val viewModel = NewMessageViewModel(contacts)

        assertEquals(listOf("Alice Adams"), viewModel.stateFor("ali").matches.map { it.displayName })
        assertEquals(listOf("Bob Baker"), viewModel.stateFor("7654").matches.map { it.displayName })
    }

    @Test
    fun acceptsManualNumberWhenValidEnough() {
        val viewModel = NewMessageViewModel(contacts)

        assertEquals("+15551234567", viewModel.manualRecipient("+1 (555) 123-4567")?.address)
        assertNull(viewModel.manualRecipient("555"))
    }
}
