package com.kheyr.sms.sync

import java.security.MessageDigest
import java.util.Base64

/** Creates salted phone identifiers so sync metadata can avoid raw phone numbers where possible. */
class PhoneIdentifierHasher(private val salt: String) {
    init { require(salt.isNotBlank()) { "Salt is required" } }

    fun hash(phoneNumber: String): String {
        val normalized = phoneNumber.filter { it.isDigit() || it == '+' }
        val digest = MessageDigest.getInstance("SHA-256").digest("$salt:$normalized".toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
