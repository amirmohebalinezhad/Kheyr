package com.kheyr.sms.ui

import com.kheyr.sms.contacts.DeviceContact
import com.kheyr.sms.contacts.PhoneNumberNormalizer

data class NewMessageRecipient(
    val displayName: String,
    val address: String,
    val photoUri: android.net.Uri? = null,
)

data class NewMessageUiState(
    val query: String = "",
    val matches: List<DeviceContact> = emptyList(),
    val manualAddress: String? = null,
)

class NewMessageViewModel(
    private val allContacts: List<DeviceContact> = emptyList(),
) {
    fun stateFor(query: String): NewMessageUiState {
        val trimmed = query.trim()
        val normalizedQuery = PhoneNumberNormalizer.normalize(trimmed)
        val matches = if (trimmed.isBlank()) {
            allContacts
        } else {
            allContacts.filter { contact ->
                contact.displayName.contains(trimmed, ignoreCase = true) ||
                    contact.phoneNumber.contains(trimmed, ignoreCase = true) ||
                    PhoneNumberNormalizer.normalize(contact.phoneNumber).contains(normalizedQuery)
            }
        }
        return NewMessageUiState(
            query = query,
            matches = matches,
            manualAddress = normalizedManualAddress(trimmed),
        )
    }

    fun recipientFor(contact: DeviceContact): NewMessageRecipient = NewMessageRecipient(
        displayName = contact.displayName,
        address = PhoneNumberNormalizer.normalize(contact.phoneNumber).ifBlank { contact.phoneNumber.trim() },
        photoUri = contact.photoUri,
    )

    fun manualRecipient(query: String): NewMessageRecipient? = normalizedManualAddress(query.trim())?.let { address ->
        NewMessageRecipient(displayName = address, address = address)
    }

    private fun normalizedManualAddress(input: String): String? {
        val normalized = PhoneNumberNormalizer.normalize(input)
        val digitCount = normalized.count(Char::isDigit)
        return normalized.takeIf { digitCount >= 7 && it.any(Char::isDigit) }
    }
}
