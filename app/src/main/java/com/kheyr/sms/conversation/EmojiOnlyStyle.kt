package com.kheyr.sms.conversation

data class EmojiOnlyStyle(val showBubble: Boolean, val textScale: Float, val showTimestamp: Boolean, val showStatus: Boolean)
class EmojiOnlyStyleResolver {
    fun resolve(isEmojiOnly: Boolean, outgoing: Boolean): EmojiOnlyStyle = if (isEmojiOnly) EmojiOnlyStyle(false, 1.8f, true, outgoing) else EmojiOnlyStyle(true, 1.0f, true, outgoing)
}
