package com.kheyr.sms.spam

import com.kheyr.sms.domain.UserSpamCorrection

data class SpamFeedbackHistory(val entries: List<UserSpamCorrection>) {
    val count: Int get() = entries.size
}
