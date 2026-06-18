package com.kheyr.sms.ui

import com.kheyr.sms.data.MessageDirection
import com.kheyr.sms.data.MessageStatus
import com.kheyr.sms.data.SmsMessage
import com.kheyr.sms.data.SmsThread
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationScreenMapperTest {
    private val mapper = ConversationScreenMapper()

    @Test fun sentOutgoingMessageDoesNotShowRetry() {
        val row = mapper.map(thread(), listOf(outgoing(MessageStatus.Sent, "hello")), emptyList(), SmsComposerState()).messages.single()

        assertFalse(row.showRetry)
        assertNull(row.copyableCode)
    }

    @Test fun deliveredOutgoingMessageDoesNotShowRetry() {
        val row = mapper.map(thread(), listOf(outgoing(MessageStatus.Delivered, "hello")), emptyList(), SmsComposerState()).messages.single()

        assertFalse(row.showRetry)
    }

    @Test fun failedOutgoingMessageShowsRetry() {
        val row = mapper.map(thread(), listOf(outgoing(MessageStatus.Failed, "hello")), emptyList(), SmsComposerState()).messages.single()

        assertTrue(row.showRetry)
        assertNull(row.copyableCode)
    }

    @Test fun incomingOtpMessageShowsCopyCode() {
        val row = mapper.map(
            thread(),
            listOf(incoming("Your verification code is 123456")),
            emptyList(),
            SmsComposerState(),
        ).messages.single()

        assertFalse(row.showRetry)
        assertTrue(row.copyableCode == "123456")
    }

    private fun thread() = SmsThread(
        id = 1L,
        address = "+15551234567",
        displayName = "Alice",
        lastMessage = "",
        lastMessageAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun outgoing(status: MessageStatus, body: String) = SmsMessage(
        id = 1L,
        threadId = 1L,
        address = "+15551234567",
        body = body,
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
        direction = MessageDirection.Outgoing,
        status = status,
    )

    private fun incoming(body: String) = SmsMessage(
        id = 2L,
        threadId = 1L,
        address = "+15551234567",
        body = body,
        timestamp = Instant.parse("2026-01-01T00:00:01Z"),
        direction = MessageDirection.Incoming,
        status = MessageStatus.Received,
    )
}
