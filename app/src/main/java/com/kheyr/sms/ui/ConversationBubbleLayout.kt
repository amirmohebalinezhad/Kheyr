package com.kheyr.sms.ui

import com.kheyr.sms.data.ConversationPresentation
import com.kheyr.sms.data.MessageDirection

enum class BubbleAlignment { Start, End }

data class ConversationBubbleLayout(
    val alignment: BubbleAlignment,
    val showBubble: Boolean,
    val emphasizeEmoji: Boolean,
)

object ConversationBubbleLayoutResolver {
    fun resolve(direction: MessageDirection, body: String): ConversationBubbleLayout {
        val emojiOnly = ConversationPresentation.isEmojiOnly(body)
        return ConversationBubbleLayout(
            alignment = if (direction == MessageDirection.Outgoing) BubbleAlignment.End else BubbleAlignment.Start,
            showBubble = !emojiOnly,
            emphasizeEmoji = emojiOnly,
        )
    }
}
