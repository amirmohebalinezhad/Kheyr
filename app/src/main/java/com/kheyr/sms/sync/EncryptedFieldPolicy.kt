package com.kheyr.sms.sync

enum class SyncFieldProtection { PlainMetadata, Encrypted, SaltedHash }
object EncryptedFieldPolicy {
    fun protectionFor(fieldName: String): SyncFieldProtection = when (fieldName) {
        "body", "sender", "recipient", "address" -> SyncFieldProtection.Encrypted
        "phoneNumber" -> SyncFieldProtection.SaltedHash
        else -> SyncFieldProtection.PlainMetadata
    }
}
