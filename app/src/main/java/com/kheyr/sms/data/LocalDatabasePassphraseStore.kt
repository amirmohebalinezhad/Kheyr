package com.kheyr.sms.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import java.util.Base64

/** Stores and retrieves the SQLCipher passphrase using encrypted shared preferences. */
class LocalDatabasePassphraseStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getOrCreatePassphrase(): ByteArray {
        val existing = preferences.getString(EncryptedDatabasePolicy.passphrasePreferenceKey, null)
        if (existing != null) return Base64.getDecoder().decode(existing)
        val generated = ByteArray(PASSPHRASE_BYTES).also(SecureRandom()::nextBytes)
        preferences.edit()
            .putString(EncryptedDatabasePolicy.passphrasePreferenceKey, Base64.getEncoder().encodeToString(generated))
            .apply()
        return generated
    }

    private companion object {
        const val PREFS_NAME = "kheyr_secure_db"
        const val PASSPHRASE_BYTES = 32
    }
}
