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

    /** Inverse of [encryptBody]; the GCM tag is verified, so tampered ciphertext throws. */
    fun decryptBody(body: EncryptedSmsBody): String {
        val nonce = Base64.getDecoder().decode(body.nonceBase64)
        val ciphertext = Base64.getDecoder().decode(body.ciphertextBase64)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    /** Decrypts a [EncryptedSmsBody.wireFormat] blob (e.g. a relayed desktop SMS body/number). */
    fun decryptWireFormat(wire: String): String =
        decryptBody(EncryptedSmsBody.parse(wire) ?: error("Malformed encrypted payload"))

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
) {
    /**
     * Self-describing wire form "algorithm.nonceBase64.ciphertextBase64". The GCM nonce MUST travel
     * with the ciphertext or no receiving device can decrypt. Standard base64 never contains '.', so
     * it is a safe delimiter; the zero-knowledge backend stores this blob opaquely and the decrypting
     * device (e.g. a paired desktop) splits it back into its three parts.
     */
    fun wireFormat(): String = "$algorithm.$nonceBase64.$ciphertextBase64"

    companion object {
        /** Splits a [wireFormat] blob back into its three parts; null if the shape is not recognized. */
        fun parse(wire: String): EncryptedSmsBody? {
            val parts = wire.split('.')
            if (parts.size != 3 || parts.any(String::isBlank)) return null
            return EncryptedSmsBody(algorithm = parts[0], nonceBase64 = parts[1], ciphertextBase64 = parts[2])
        }
    }
}
