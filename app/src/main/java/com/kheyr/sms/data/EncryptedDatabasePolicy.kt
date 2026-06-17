package com.kheyr.sms.data

/** Describes encrypted local storage requirements for SMS data at rest. */
enum class LocalStorageStrategy { SqlCipherPassphrase, EncryptedFileBacked }

object EncryptedDatabasePolicy {
    val strategy: LocalStorageStrategy = LocalStorageStrategy.SqlCipherPassphrase
    val databaseFileName: String = "kheyr_sms.db"
    val passphrasePreferenceKey: String = "local_db_passphrase"

    fun requiresEncryptionAtRest(): Boolean = true

    fun encryptedFields(): Set<String> = setOf("body", "address", "displayName")
}
