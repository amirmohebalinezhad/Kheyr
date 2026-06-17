package com.kheyr.sms.sync.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts SMS bodies before sync payloads are built. */
class SmsBodyEncryptor(
    private val key: SecretKey,
    private val random: SecureRandom = SecureRandom(),
) {
    fun encryptBody(plaintextBody: String): EncryptedSmsBody {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        val ciphertext = cipher.doFinal(plaintextBody.toByteArray(Charsets.UTF_8))
        return EncryptedSmsBody(
            algorithm = ALGORITHM_NAME,
            nonceBase64 = Base64.getEncoder().encodeToString(nonce),
            ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext),
        )
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ALGORITHM_NAME = "AES-256-GCM"
        private const val NONCE_BYTES = 12
        private const val TAG_BITS = 128
    }
}

data class EncryptedSmsBody(
    val algorithm: String,
    val nonceBase64: String,
    val ciphertextBase64: String,
)
