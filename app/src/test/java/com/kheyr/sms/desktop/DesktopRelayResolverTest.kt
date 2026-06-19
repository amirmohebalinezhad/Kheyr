package com.kheyr.sms.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopRelayResolverTest {
    private fun payload(body: String = "enc-body", target: String = "enc-target", sim: String? = "2") =
        DesktopRelayPayload("req-1", body, target, sim, threadId = 7L, clientMessageId = "c-1")

    @Test fun decryptsAndResolvesSim() {
        val resolver = DesktopRelayResolver(
            decrypt = { value -> value.removePrefix("enc-") },
            resolveSubscriptionId = { it?.toIntOrNull() },
        )
        val result = resolver.resolve(payload())
        assertTrue(result is DesktopRelayResolution.Send)
        result as DesktopRelayResolution.Send
        assertEquals("target", result.recipient)
        assertEquals("body", result.body)
        assertEquals(2, result.subscriptionId)
        assertEquals(7L, result.threadId)
    }

    @Test fun rejectsWhenRecipientUndecryptable() {
        val resolver = DesktopRelayResolver(
            decrypt = { value -> if (value.contains("target")) error("bad") else "body" },
            resolveSubscriptionId = { null },
        )
        assertEquals(
            DesktopRelayResolution.Reject("req-1", "Could not read recipient"),
            resolver.resolve(payload()),
        )
    }

    @Test fun rejectsWhenBodyBlank() {
        val resolver = DesktopRelayResolver(
            decrypt = { value -> if (value.contains("body")) "" else "+15551234567" },
            resolveSubscriptionId = { null },
        )
        assertEquals(
            DesktopRelayResolution.Reject("req-1", "Could not read message body"),
            resolver.resolve(payload()),
        )
    }

    @Test fun fallsBackToDefaultSimWhenNull() {
        val resolver = DesktopRelayResolver(
            decrypt = { it.removePrefix("enc-") },
            resolveSubscriptionId = { it?.toIntOrNull() ?: 99 },
        )
        val result = resolver.resolve(payload(sim = null)) as DesktopRelayResolution.Send
        assertEquals(99, result.subscriptionId)
    }
}
