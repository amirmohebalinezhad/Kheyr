package com.kheyr.sms.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

data class SyncEncryptionKeyStoreState(val hasKey: Boolean, val alias: String = SyncEncryptionKeyStore.KEY_ALIAS) {
    val readyForUpload: Boolean get() = hasKey
}

/**
 * Holds the symmetric AES-256 key used to encrypt synced SMS bodies. The key is random per install
 * and stored at rest in [EncryptedSharedPreferences] (protected by the AndroidKeyStore master key).
 *
 * Unlike a raw, non-exportable AndroidKeyStore key, this key is intentionally readable by the app:
 * synced ciphertext is decrypted by the user's OTHER devices (e.g. a paired desktop), so the content
 * key must be shareable. The desktop-pairing flow is expected to wrap [keyMaterial] with the paired
 * device's public key ([DesktopPairingQrPayload.desktopPublicKeyBase64]) before transferring it; the
 * key is never uploaded in the clear and the zero-knowledge backend never receives it in any form.
 *
 * Reading/creating the store is resilient (mirroring [com.kheyr.sms.data.LocalDatabasePassphraseStore])
 * because the shared AndroidKeyStore master key can be invalidated or its backing prefs file
 * corrupted; in that case the store resets and regenerates the key rather than throwing.
 */
class SyncEncryptionKeyStore(private val context: Context) {

    /** Raw 256-bit key bytes, generating and persisting them on first use. Shareable during pairing. */
    fun keyMaterial(): ByteArray {
        val prefs = openPreferencesResilient()
        return try {
            readOrGenerate(prefs)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read/write sync key; resetting key store", e)
            readOrGenerate(resetAndRecreate())
        }
    }

    /** The content key in the form [com.kheyr.sms.sync.crypto.SmsBodyEncryptor] consumes. */
    fun getOrCreateKey(): SecretKey = SecretKeySpec(keyMaterial(), "AES")

    fun state(): SyncEncryptionKeyStoreState =
        SyncEncryptionKeyStoreState(hasKey = runCatching { openPreferencesResilient().contains(KEY_ALIAS) }.getOrDefault(false))

    private fun readOrGenerate(prefs: SharedPreferences): ByteArray {
        prefs.getString(KEY_ALIAS, null)?.let { return Base64.getDecoder().decode(it) }
        val generated = ByteArray(KEY_SIZE_BYTES).also(SecureRandom()::nextBytes)
        prefs.edit().putString(KEY_ALIAS, Base64.getEncoder().encodeToString(generated)).apply()
        return generated
    }

    private fun openPreferencesResilient(): SharedPreferences = try {
        createEncryptedPreferences()
    } catch (e: Exception) {
        Log.w(TAG, "Failed to open sync key store; recreating it", e)
        resetAndRecreate()
    }

    private fun resetAndRecreate(): SharedPreferences {
        // Only this store's own prefs file is cleared. The shared master key is left alone so the
        // database passphrase store (which owns master-key recovery) is not disrupted.
        runCatching { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply() }
        runCatching {
            val dir = File(context.applicationInfo.dataDir, "shared_prefs")
            File(dir, "$PREFS_NAME.xml").delete()
            File(dir, "$PREFS_NAME.xml.bak").delete()
        }
        return createEncryptedPreferences()
    }

    private fun createEncryptedPreferences(): SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    companion object {
        const val KEY_ALIAS = "kheyr_sync_key"
        private const val TAG = "SyncKeyStore"
        private const val PREFS_NAME = "kheyr_sync_key_store"
        private const val KEY_SIZE_BYTES = 32
    }
}
