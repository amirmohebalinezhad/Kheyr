package com.kheyr.sms.desktop

/** Decoded fields of a desktop pairing QR (`{ session_id, server, expires_at }`). */
data class PairingQrContent(
    val sessionId: String?,
    val server: String?,
    val expiresAtEpochSeconds: Long?,
)

sealed interface PairingQrResult {
    data class Valid(val sessionId: String) : PairingQrResult
    data class Invalid(val reason: String) : PairingQrResult
}

/** Pure validation of a scanned pairing QR: must be a Kheyr code, not from a foreign server, not expired. */
object PairingQrValidator {
    const val SERVER = "kheyr"

    fun validate(content: PairingQrContent?, nowEpochSeconds: Long): PairingQrResult = when {
        content?.sessionId.isNullOrBlank() -> PairingQrResult.Invalid("Not a Kheyr pairing code")
        content.server != null && content.server != SERVER -> PairingQrResult.Invalid("Unrecognized pairing server")
        content.expiresAtEpochSeconds != null && content.expiresAtEpochSeconds <= nowEpochSeconds ->
            PairingQrResult.Invalid("Pairing code expired — refresh it on the desktop")
        else -> PairingQrResult.Valid(content.sessionId!!)
    }
}
