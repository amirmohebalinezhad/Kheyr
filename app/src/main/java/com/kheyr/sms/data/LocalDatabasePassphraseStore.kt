package com.kheyr.sms.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64

/**
 * Stores and retrieves the SQLCipher passphrase using encrypted shared preferences.
 *
 * The underlying [EncryptedSharedPreferences] / Keystore master key can become unusable if the
 * device's Keystore entry is invalidated (e.g. credential changes, biometric enrollment, OS
 * upgrades) or if the encrypted prefs file is corrupted. In those cases creating or reading the
 * store throws (InvalidKeyException / AEADBadTagException / IllegalStateException / GeneralSecurity
 * exceptions). Rather than crash-looping on launch, this store recovers by discarding the corrupt
 * prefs file and master key entry and recreating a fresh store. The previously stored random
 * passphrase is unrecoverable in that scenario (it only ever lived inside the now-unreadable store),
 * so the existing encrypted database will be reset/re-keyed on the next open — this is the existing
 * reality of a lost random passphrase, not a regression.
 */
class LocalDatabasePassphraseStore(private val context: Context) {

    fun getOrCreatePassphrase(): ByteArray {
        val preferences = openPreferencesResilient()
        return try {
            readOrGeneratePassphrase(preferences)
        } catch (e: Exception) {
            // Reading/writing the existing entry failed (e.g. AEADBadTagException on a corrupt
            // value). Reset the store and start fresh.
            Log.w(TAG, "Failed to read/write passphrase; resetting passphrase store", e)
            val freshPreferences = resetAndRecreatePreferences()
            readOrGeneratePassphrase(freshPreferences)
        }
    }

    private fun readOrGeneratePassphrase(preferences: SharedPreferences): ByteArray {
        val existing = preferences.getString(EncryptedDatabasePolicy.passphrasePreferenceKey, null)
        if (existing != null) return Base64.getDecoder().decode(existing)
        val generated = ByteArray(PASSPHRASE_BYTES).also(SecureRandom()::nextBytes)
        preferences.edit()
            .putString(EncryptedDatabasePolicy.passphrasePreferenceKey, Base64.getEncoder().encodeToString(generated))
            .apply()
        return generated
    }

    /**
     * Creates the [EncryptedSharedPreferences]. If creation itself fails (corrupt prefs file or an
     * invalidated/unreadable master key), the corrupt state is cleared and creation is retried once
     * against a freshly created store.
     */
    private fun openPreferencesResilient(): SharedPreferences = try {
        createEncryptedPreferences()
    } catch (e: Exception) {
        Log.w(TAG, "Failed to open encrypted passphrase store; recreating it", e)
        resetAndRecreatePreferences()
    }

    private fun resetAndRecreatePreferences(): SharedPreferences {
        clearCorruptState()
        return createEncryptedPreferences()
    }

    private fun createEncryptedPreferences(): SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    /** Deletes the encrypted prefs file and the Keystore master key so a clean store can be built. */
    private fun clearCorruptState() {
        try {
            // Clear the in-memory cache (best-effort) before deleting the backing file.
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to clear cached prefs values", e)
        }

        try {
            deletePrefsFile()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to delete corrupt prefs file", e)
        }

        try {
            deleteMasterKeyEntry()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to delete master key entry", e)
        }
    }

    private fun deletePrefsFile() {
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        File(prefsDir, "$PREFS_NAME.xml").delete()
        // SharedPreferences may also leave a write-ahead/backup file behind.
        File(prefsDir, "$PREFS_NAME.xml.bak").delete()
    }

    private fun deleteMasterKeyEntry() {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        if (keyStore.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
            keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        }
    }

    private companion object {
        const val TAG = "PassphraseStore"
        const val PREFS_NAME = "kheyr_secure_db"
        const val PASSPHRASE_BYTES = 32
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
    }
}
