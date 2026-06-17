package com.kheyr.sms.thread

data class ThreadBulkActionRequest(val threadIds: Set<Long>, val action: ThreadBulkAction) {
    val isRunnable: Boolean get() = threadIds.isNotEmpty()
}
enum class ThreadBulkAction { Delete, Archive, MarkSpam, MarkRead, Mute }
