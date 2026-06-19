package com.kheyr.sms.sync.crypto

import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsBodyEncryptorTest {
    private val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")
    private val encryptor = SmsBodyEncryptor(key)

    @Test fun encryptThenDecryptRoundTrips() {
        val plaintext = "Meet at 7? کد تایید 12345"
        val encrypted = encryptor.encryptBody(plaintext)
        assertEquals(plaintext, encryptor.decryptBody(encrypted))
    }

    @Test fun decryptWireFormatRoundTrips() {
        val wire = encryptor.encryptBody("hello world").wireFormat()
        assertEquals("hello world", encryptor.decryptWireFormat(wire))
    }

    @Test fun parseRejectsMalformedWireForms() {
        assertNull(EncryptedSmsBody.parse("only.two"))
        assertNull(EncryptedSmsBody.parse("a..c"))
        assertNull(EncryptedSmsBody.parse("plain-text-no-dots"))
    }

    @Test fun parseAcceptsWellFormedWire() {
        val body = EncryptedSmsBody.parse("AES-256-GCM.bm9uY2U.Y2lwaGVy")
        assertEquals("AES-256-GCM", body?.algorithm)
        assertEquals("bm9uY2U", body?.nonceBase64)
        assertEquals("Y2lwaGVy", body?.ciphertextBase64)
    }
}
