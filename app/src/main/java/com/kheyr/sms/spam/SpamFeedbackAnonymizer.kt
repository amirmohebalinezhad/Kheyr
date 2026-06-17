package com.kheyr.sms.spam

data class RawSpamFeedback(val senderHash: String, val body: String, val markedSpam: Boolean)
data class AnonymousSpamFeedback(val senderHash: String, val bodyLength: Int, val markedSpam: Boolean)
object SpamFeedbackAnonymizer {
    fun anonymize(feedback: RawSpamFeedback): AnonymousSpamFeedback = AnonymousSpamFeedback(feedback.senderHash, feedback.body.length, feedback.markedSpam)
}
