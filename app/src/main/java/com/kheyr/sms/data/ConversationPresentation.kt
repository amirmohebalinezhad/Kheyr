package com.kheyr.sms.data

/** Presentation helpers for conversation rows that are independent from Compose. */
object ConversationPresentation {
    fun isEmojiOnly(body: String): Boolean {
        val codePoints = body.trim().codePoints().toArray()
        if (codePoints.isEmpty()) return false
        return codePoints.all { it.isEmojiCodePoint() || it.isEmojiModifierOrJoiner() || Character.isWhitespace(it) }
    }

    fun bubbleStyleFor(body: String): MessageBubbleStyle = if (isEmojiOnly(body)) {
        MessageBubbleStyle.EmojiOnly
    } else {
        MessageBubbleStyle.StandardBubble
    }

    private fun Int.isEmojiCodePoint(): Boolean = this in 0x1F300..0x1FAFF || this in 0x2600..0x27BF

    private fun Int.isEmojiModifierOrJoiner(): Boolean = this == 0xFE0F || this == 0x200D || this in 0x1F3FB..0x1F3FF
}

enum class MessageBubbleStyle { StandardBubble, EmojiOnly }
