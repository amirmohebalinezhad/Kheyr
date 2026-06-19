package com.kheyr.sms.desktop

import org.json.JSONObject

/** Parses the raw QR string (JSON produced by the backend pairing session) into [PairingQrContent]. */
object PairingQrCodec {
    fun parse(raw: String): PairingQrContent? = runCatching {
        val json = JSONObject(raw)
        PairingQrContent(
            sessionId = json.optString("session_id").takeIf { it.isNotBlank() },
            server = json.optString("server").takeIf { it.isNotBlank() },
            expiresAtEpochSeconds = json.optLong("expires_at").takeIf { it > 0L },
        )
    }.getOrNull()
}
