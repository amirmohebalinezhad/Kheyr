package com.kheyr.sms.ui

class SmsComposerStateReducer {
    fun reduce(state: SmsComposerState, event: SmsComposerEvent): SmsComposerState = when (event) {
        is SmsComposerEvent.BodyChanged -> state.copy(body = event.body, error = null)
        is SmsComposerEvent.SubscriptionSelected -> state.copy(selectedSubscriptionId = event.subscriptionId, error = null)
        SmsComposerEvent.SendRequested -> when {
            state.body.isBlank() -> state.copy(error = ComposerError.EmptyBody)
            state.requiresSimSelection && state.selectedSubscriptionId == null -> state.copy(error = ComposerError.MissingSimSelection)
            else -> state.copy(sending = true, error = null)
        }
        SmsComposerEvent.SendCompleted -> state.copy(body = "", sending = false, error = null)
        SmsComposerEvent.SendFailed -> state.copy(sending = false, error = ComposerError.SendFailed)
    }
}

data class SmsComposerState(
    val body: String = "",
    val selectedSubscriptionId: Int? = null,
    val requiresSimSelection: Boolean = false,
    val sending: Boolean = false,
    val error: ComposerError? = null,
)

sealed interface SmsComposerEvent {
    data class BodyChanged(val body: String) : SmsComposerEvent
    data class SubscriptionSelected(val subscriptionId: Int) : SmsComposerEvent
    data object SendRequested : SmsComposerEvent
    data object SendCompleted : SmsComposerEvent
    data object SendFailed : SmsComposerEvent
}

enum class ComposerError { EmptyBody, MissingSimSelection, SendFailed }
