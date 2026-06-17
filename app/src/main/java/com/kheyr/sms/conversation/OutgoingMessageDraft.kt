package com.kheyr.sms.conversation

data class OutgoingMessageDraft(val recipients: List<String>, val body: String, val subscriptionId: Int?) {
    fun validationErrors(requireSimSelection: Boolean): Set<DraftError> = buildSet {
        if (recipients.none { it.isNotBlank() }) add(DraftError.MissingRecipient)
        if (body.isBlank()) add(DraftError.EmptyBody)
        if (requireSimSelection && subscriptionId == null) add(DraftError.MissingSim)
    }
}
enum class DraftError { MissingRecipient, EmptyBody, MissingSim }
