package com.kheyr.sms.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object SmsRefreshEvents {
    private val _events = MutableSharedFlow<RefreshKind>(extraBufferCapacity = 8)
    val events: SharedFlow<RefreshKind> = _events

    sealed interface RefreshKind {
        data object Threads : RefreshKind
        data class ThreadMessages(val threadId: Long) : RefreshKind
    }

    fun notifyThreadsChanged() {
        _events.tryEmit(RefreshKind.Threads)
    }

    fun notifyThreadChanged(threadId: Long) {
        _events.tryEmit(RefreshKind.ThreadMessages(threadId))
        _events.tryEmit(RefreshKind.Threads)
    }
}
