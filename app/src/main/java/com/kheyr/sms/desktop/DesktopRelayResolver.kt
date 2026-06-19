package com.kheyr.sms.desktop

/** Outcome of turning an encrypted [DesktopRelayPayload] into something the phone can act on. */
sealed interface DesktopRelayResolution {
    data class Send(
        val requestId: String,
        val recipient: String,
        val body: String,
        val subscriptionId: Int?,
        val threadId: Long?,
    ) : DesktopRelayResolution

    data class Reject(val requestId: String, val reason: String) : DesktopRelayResolution
}

/**
 * Pure logic for a relayed desktop SMS: decrypt the recipient/body and resolve the target SIM.
 * Android concerns (actually decrypting with the keystore key, reading default-SIM prefs, sending,
 * reporting status) are injected so this stays unit-testable. See [DesktopRelayExecutor] for the glue.
 */
class DesktopRelayResolver(
    private val decrypt: (String) -> String,
    private val resolveSubscriptionId: (String?) -> Int?,
) {
    fun resolve(payload: DesktopRelayPayload): DesktopRelayResolution {
        val recipient = runCatching { decrypt(payload.encryptedTargetNumber) }.getOrNull()
        val body = runCatching { decrypt(payload.encryptedBody) }.getOrNull()
        return when {
            recipient.isNullOrBlank() -> DesktopRelayResolution.Reject(payload.requestId, "Could not read recipient")
            body.isNullOrBlank() -> DesktopRelayResolution.Reject(payload.requestId, "Could not read message body")
            else -> DesktopRelayResolution.Send(
                requestId = payload.requestId,
                recipient = recipient,
                body = body,
                subscriptionId = resolveSubscriptionId(payload.simId),
                threadId = payload.threadId,
            )
        }
    }
}
