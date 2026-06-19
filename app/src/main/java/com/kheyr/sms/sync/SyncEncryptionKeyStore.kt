package com.kheyr.sms.sync

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

data class SyncEncryptionKeyStoreState(val hasKey: Boolean, val alias: String = SyncEncryptionKeyStore.KEY_ALIAS) {
    val readyForUpload: Boolean get() = hasKey
}

/**
 * Provides the per-device AES-256 key used to encrypt synced SMS bodies. The key never leaves the
 * AndroidKeyStore and is generated once per install under [KEY_ALIAS]. The key is created with
 * randomized-encryption disabled because [com.kheyr.sms.sync.crypto.SmsBodyEncryptor] supplies its
 * own random 12-byte GCM IV per message; with randomized encryption required the Keystore would
 * reject a caller-provided IV at encrypt time.
 */
object SyncEncryptionKeyStore {
    const val KEY_ALIAS = "kheyr_sync_key"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val KEY_SIZE_BITS = 256

    /** Returns the existing AES key for [KEY_ALIAS], generating and persisting one if absent. */
    fun getOrCreateKey(alias: String = KEY_ALIAS): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return generateKey(alias)
    }

    fun state(alias: String = KEY_ALIAS): SyncEncryptionKeyStoreState {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        return SyncEncryptionKeyStoreState(hasKey = keyStore.containsAlias(alias), alias = alias)
    }

    private fun generateKey(alias: String): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .setRandomizedEncryptionRequired(false)
                .build(),
        )
        return generator.generateKey()
    }
}
