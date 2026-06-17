package com.kheyr.sms.sync

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopSmsSendGateTest {
    private val gate = DesktopSmsSendGate()
    private val device = DesktopDevice("desktop-1", "Workstation", DesktopPlatform.Linux, Instant.EPOCH)

    @Test fun supportedDesktopPlatformsIncludeRequiredTargets() {
        assertEquals(setOf(DesktopPlatform.Windows, DesktopPlatform.MacOS, DesktopPlatform.Linux), DesktopPlatform.entries.toSet())
    }

    @Test fun qrPairingPayloadExpiresAtRequestedDeadline() {
        val payload = DesktopPairingQrPayload(
            pairingId = "pair-1",
            desktopPublicKeyBase64 = "public-key",
            requestedAt = Instant.parse("2026-01-01T00:00:00Z"),
            expiresAt = Instant.parse("2026-01-01T00:05:00Z"),
        )

        assertEquals(false, payload.isExpiredAt(Instant.parse("2026-01-01T00:04:59Z")))
        assertEquals(true, payload.isExpiredAt(Instant.parse("2026-01-01T00:05:00Z")))
    }

    @Test fun offlinePhoneShowsWaitingForPhone() {
        val decision = gate.evaluate(device, PhonePresence(online = false, lastSeenAt = Instant.EPOCH), request())

        assertEquals(DesktopSmsSendDecision.WaitingForPhone, decision)
    }

    @Test fun onlinePhoneForwardsValidSendRequest() {
        val decision = gate.evaluate(device, PhonePresence(online = true, lastSeenAt = Instant.EPOCH), request())

        assertTrue(decision is DesktopSmsSendDecision.ForwardToPhone)
        assertEquals("+15551234567", (decision as DesktopSmsSendDecision.ForwardToPhone).request.recipient)
    }

    @Test fun revokedDesktopDeviceCannotSend() {
        val revoked = device.copy(revokedAt = Instant.EPOCH)
        val decision = gate.evaluate(revoked, PhonePresence(online = true, lastSeenAt = Instant.EPOCH), request())

        assertEquals(DesktopSmsSendDecision.Rejected("Desktop device is revoked"), decision)
    }

    @Test fun blankRecipientOrBodyIsRejectedBeforeForwarding() {
        assertEquals(
            DesktopSmsSendDecision.Rejected("Recipient is required"),
            gate.evaluate(device, PhonePresence(online = true, lastSeenAt = Instant.EPOCH), request(recipient = "")),
        )
        assertEquals(
            DesktopSmsSendDecision.Rejected("Message body is required"),
            gate.evaluate(device, PhonePresence(online = true, lastSeenAt = Instant.EPOCH), request(body = "")),
        )
    }

    private fun request(recipient: String = "+15551234567", body: String = "hello") = DesktopSmsSendRequest(
        requestId = "req-1",
        recipient = recipient,
        body = body,
        subscriptionId = 2,
    )
}
