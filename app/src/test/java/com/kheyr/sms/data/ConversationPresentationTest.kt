package com.kheyr.sms.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationPresentationTest {
    @Test fun emojiOnlyMessagesUseEmojiOnlyStyle() {
        assertTrue(ConversationPresentation.isEmojiOnly("🎉😊"))
        assertEquals(MessageBubbleStyle.EmojiOnly, ConversationPresentation.bubbleStyleFor("👍🏽"))
    }

    @Test fun mixedTextMessagesUseStandardBubble() {
        assertFalse(ConversationPresentation.isEmojiOnly("hello 😊"))
        assertEquals(MessageBubbleStyle.StandardBubble, ConversationPresentation.bubbleStyleFor("hello 😊"))
    }
}
