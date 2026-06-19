package com.kheyr.sms.ui

import com.kheyr.sms.thread.ThreadBulkAction
import com.kheyr.sms.thread.ThreadBulkActionRequest

data class ThreadSelectionState(val selectedThreadIds: Set<Long> = emptySet()) {
    val isSelectionMode: Boolean get() = selectedThreadIds.isNotEmpty()
    fun isSelected(threadId: Long): Boolean = threadId in selectedThreadIds
    fun select(threadId: Long): ThreadSelectionState = copy(selectedThreadIds = selectedThreadIds + threadId)
    fun toggle(threadId: Long): ThreadSelectionState = copy(
        selectedThreadIds = if (threadId in selectedThreadIds) selectedThreadIds - threadId else selectedThreadIds + threadId,
    )
    fun clear(): ThreadSelectionState = copy(selectedThreadIds = emptySet())
    fun request(action: ThreadBulkAction): ThreadBulkActionRequest = ThreadBulkActionRequest(selectedThreadIds, action)
}
