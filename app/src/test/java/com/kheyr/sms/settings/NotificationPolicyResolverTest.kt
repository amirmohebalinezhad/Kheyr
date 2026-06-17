package com.kheyr.sms.settings

import com.kheyr.sms.domain.SpamClassification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPolicyResolverTest {
    private val resolver = NotificationPolicyResolver()

    @Test fun spamAndBlockedSendersAreSuppressed() {
        assertEquals(NotificationPolicyDecision.Suppress, resolver.resolve(input(spamClassification = SpamClassification.Spam)))
        assertEquals(NotificationPolicyDecision.Suppress, resolver.resolve(input(senderBlocked = true)))
    }

    @Test fun unknownSenderNoneSuppressesBeforeSoundSelection() {
        val decision = resolver.resolve(input(
            senderIsContact = false,
            globalSettings = NotificationSettings(unknownSenderMode = UnknownSenderNotificationMode.None),
            threadSettings = ThreadNotificationSettings(threadId = 7, customRingtoneUri = "content://ringtone"),
        ))

        assertEquals(NotificationPolicyDecision.Suppress, decision)
    }

    @Test fun threadMuteAndUnknownSilentForceSilentWithoutVibration() {
        val muted = resolver.resolve(input(threadSettings = ThreadNotificationSettings(threadId = 7, muted = true)))
        val unknownSilent = resolver.resolve(input(
            senderIsContact = false,
            globalSettings = NotificationSettings(unknownSenderMode = UnknownSenderNotificationMode.Silent),
        ))

        assertTrue(muted is NotificationPolicyDecision.Post)
        assertEquals(NotificationSoundMode.Silent, (muted as NotificationPolicyDecision.Post).soundMode)
        assertEquals(false, muted.vibrate)
        assertTrue(unknownSilent is NotificationPolicyDecision.Post)
        assertEquals(NotificationSoundMode.Silent, (unknownSilent as NotificationPolicyDecision.Post).soundMode)
    }

    @Test fun threadRingtoneOverridesGlobalRingtone() {
        val decision = resolver.resolve(input(
            globalSettings = NotificationSettings(globalRingtoneUri = "content://global"),
            threadSettings = ThreadNotificationSettings(threadId = 7, customRingtoneUri = "content://thread"),
        )) as NotificationPolicyDecision.Post

        assertEquals(NotificationSoundMode.Custom("content://thread"), decision.soundMode)
    }

    @Test fun contentPrivacyModesHidePreviewOrSender() {
        val senderOnly = resolver.resolve(input(
            globalSettings = NotificationSettings(contentMode = NotificationContentMode.ShowSenderOnly),
        )) as NotificationPolicyDecision.Post
        val hidden = resolver.resolve(input(
            globalSettings = NotificationSettings(contentMode = NotificationContentMode.HideSenderAndPreview),
        )) as NotificationPolicyDecision.Post

        assertEquals("Mom", senderOnly.title)
        assertNull(senderOnly.body)
        assertEquals("New message", hidden.title)
        assertEquals("Open Kheyr to read it", hidden.body)
    }

    private fun input(
        senderIsContact: Boolean = true,
        senderBlocked: Boolean = false,
        spamClassification: SpamClassification = SpamClassification.Normal,
        globalSettings: NotificationSettings = NotificationSettings(),
        threadSettings: ThreadNotificationSettings? = null,
    ) = NotificationPolicyInput(
        sender = "+15551234567",
        displayName = "Mom",
        preview = "Dinner?",
        senderIsContact = senderIsContact,
        senderBlocked = senderBlocked,
        spamClassification = spamClassification,
        globalSettings = globalSettings,
        threadSettings = threadSettings,
    )
}
