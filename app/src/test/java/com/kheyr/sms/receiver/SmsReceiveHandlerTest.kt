package com.kheyr.sms.receiver

import com.kheyr.sms.domain.SpamRule
import com.kheyr.sms.domain.SpamRuleSet
import com.kheyr.sms.domain.SpamRuleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsReceiveHandlerTest {
    @Test fun spamMessageIsPersistedAsSpamWithoutNotification() {
        val harness = Harness()
        val result = harness.handle(IncomingSms("+989991234", "winner visit https://bad.example", 10L, 1, 7))

        assertEquals(IncomingSmsResult.SpamSuppressed, result)
        assertEquals(1, harness.spamMessages.size)
        assertEquals(0, harness.inboxMessages.size)
        assertEquals(0, harness.notifications)
    }

    @Test fun nonSpamMessageIsPersistedAndNotified() {
        val harness = Harness(knownContacts = setOf("+15551234567"))
        val result = harness.handle(IncomingSms("+15551234567", "Dinner at 7?", 20L, 0, 3))

        assertEquals(IncomingSmsResult.NotificationPosted, result)
        assertEquals(0, harness.spamMessages.size)
        assertEquals(1, harness.inboxMessages.size)
        assertEquals(1, harness.notifications)
    }

    @Test fun otpNegativeScorePreventsUnknownSenderSpamSuppression() {
        val harness = Harness()
        val result = harness.handle(IncomingSms("12345", "Your code is 123456", 30L, null, null))

        assertEquals(IncomingSmsResult.NotificationPosted, result)
        assertEquals(0, harness.spamMessages.size)
        assertEquals(1, harness.inboxMessages.size)
        assertEquals(1, harness.notifications)
    }

    @Test fun unknownSenderNotificationRunsOnlyAfterSpamCheck() {
        val harness = Harness()
        val result = harness.handle(IncomingSms("+15557654321", "hello from a new number", 40L, 1, 9))

        assertEquals(IncomingSmsResult.NotificationPosted, result)
        assertEquals(0, harness.spamMessages.size)
        assertEquals(1, harness.notifications)
        assertTrue(harness.lastNotificationWasUnknownSender)
    }

    private class Harness(knownContacts: Set<String> = emptySet()) {
        val spamMessages = mutableListOf<IncomingSms>()
        val inboxMessages = mutableListOf<IncomingSms>()
        var notifications = 0
        var lastNotificationWasUnknownSender = false
        private val handler = SmsReceiveHandler(
            spamRules = SpamRulesProvider { rules },
            contactLookup = SenderContactLookup { it in knownContacts },
            spamStore = SpamMessageStore { message, _, _ -> spamMessages += message },
            inboxStore = InboxMessageStore { message ->
                inboxMessages += message
                StoredIncomingSms(1L, message.sender, message.body, message.receivedAtMillis, message.simSlot, message.subscriptionId)
            },
            notifier = IncomingSmsNotifier { _, senderIsContact ->
                notifications += 1
                lastNotificationWasUnknownSender = !senderIsContact
            },
        )

        fun handle(message: IncomingSms) = handler.handle(message)

        private companion object {
            val rules = SpamRuleSet(1, threshold = 70, rules = listOf(
                SpamRule("premium-prefix", SpamRuleType.NumberPrefix, "+98999", 35),
                SpamRule("winner", SpamRuleType.MessageKeyword, "winner", 30),
                SpamRule("url", SpamRuleType.UrlDetected, score = 40),
                SpamRule("unknown", SpamRuleType.SenderNotInContacts, score = 45),
                SpamRule("otp", SpamRuleType.OtpRegex, "\\b\\d{4,8}\\b", -40),
            ))
        }
    }
}
