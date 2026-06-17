package com.kheyr.sms.direct

data class ContactRegistrationLookup(val address: String, val isRegistered: Boolean) {
    val supportsDirectMessage: Boolean get() = isRegistered
}
