package com.kheyr.sms.spam

data class SpamRestorePlan(val messageId: Long, val moveToInbox: Boolean, val syncCorrection: Boolean)
class SpamRestorePlanner {
    fun plan(messageId: Long, syncEnabled: Boolean): SpamRestorePlan = SpamRestorePlan(messageId, moveToInbox = true, syncCorrection = syncEnabled)
}
