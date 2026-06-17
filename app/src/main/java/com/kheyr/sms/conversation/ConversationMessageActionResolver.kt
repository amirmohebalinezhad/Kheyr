package com.kheyr.sms.conversation

data class ConversationMessageActionAvailability(val canCopy: Boolean, val canDelete: Boolean, val canMarkSpam: Boolean, val canMarkNotSpam: Boolean, val canRetry: Boolean)
class ConversationMessageActionResolver {
    fun resolve(isSpam: Boolean, failedOutgoing: Boolean): ConversationMessageActionAvailability = ConversationMessageActionAvailability(true, true, !isSpam, isSpam, failedOutgoing)
}
