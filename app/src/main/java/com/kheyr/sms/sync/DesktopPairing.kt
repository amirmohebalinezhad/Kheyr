package com.kheyr.sms.sync

import java.time.Instant

/** Pure models for desktop pairing and phone-mediated desktop SMS sends. */
data class DesktopPairingQrPayload(
    val pairingId: String,
    val desktopPublicKeyBase64: String,
    val requestedAt: Instant,
    val expiresAt: Instant,
) {
    fun isExpiredAt(now: Instant): Boolean = !expiresAt.isAfter(now)
}

data class DesktopDevice(
    val id: String,
    val displayName: String,
    val platform: DesktopPlatform,
    val pairedAt: Instant,
    val revokedAt: Instant? = null,
) {
    val canSync: Boolean get() = revokedAt == null
}

enum class DesktopPlatform { Windows, MacOS, Linux }

data class PhonePresence(val online: Boolean, val lastSeenAt: Instant?)

data class DesktopSmsSendRequest(
    val requestId: String,
    val recipient: String,
    val body: String,
    val subscriptionId: Int?,
)

class DesktopSmsSendGate {
    fun evaluate(device: DesktopDevice, phonePresence: PhonePresence, request: DesktopSmsSendRequest): DesktopSmsSendDecision = when {
        !device.canSync -> DesktopSmsSendDecision.Rejected("Desktop device is revoked")
        request.recipient.isBlank() -> DesktopSmsSendDecision.Rejected("Recipient is required")
        request.body.isBlank() -> DesktopSmsSendDecision.Rejected("Message body is required")
        !phonePresence.online -> DesktopSmsSendDecision.WaitingForPhone
        else -> DesktopSmsSendDecision.ForwardToPhone(request)
    }
}

sealed interface DesktopSmsSendDecision {
    data object WaitingForPhone : DesktopSmsSendDecision
    data class ForwardToPhone(val request: DesktopSmsSendRequest) : DesktopSmsSendDecision
    data class Rejected(val reason: String) : DesktopSmsSendDecision
}
