package com.kheyr.sms.domain

import java.time.Instant

/** Anonymous feedback emitted when a user corrects spam classification. */
data class SpamFeedbackEvent(
    val messageId: Long,
    val threadId: Long,
    val action: SpamFeedbackAction,
    val previousClassification: SpamClassification,
    val correctedAt: Instant,
    val triggeredRuleIds: List<String> = emptyList(),
) {
    fun anonymousPayload(): AnonymousSpamFeedbackPayload = AnonymousSpamFeedbackPayload(
        action = action,
        previousClassification = previousClassification,
        correctedAt = correctedAt,
        triggeredRuleIds = triggeredRuleIds.distinct().sorted(),
    )
}

enum class SpamFeedbackAction { MarkSpam, MarkNotSpam }

data class AnonymousSpamFeedbackPayload(
    val action: SpamFeedbackAction,
    val previousClassification: SpamClassification,
    val correctedAt: Instant,
    val triggeredRuleIds: List<String>,
)
