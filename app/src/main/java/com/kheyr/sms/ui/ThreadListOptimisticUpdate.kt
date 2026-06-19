package com.kheyr.sms.ui

import com.kheyr.sms.data.SmsThread
import com.kheyr.sms.thread.ThreadBulkAction
import java.time.Instant

object ThreadListOptimisticUpdate {
    // `target` forces a fixed state for the stateful toggles (Archive/MarkSpam/Mute) so a bulk action
    // applies one consistent result across a mixed selection. When null each thread is toggled from its
    // own current state (used by the single-thread action dialog, which shows state-aware labels).
    fun applyAction(
        threads: List<SmsThread>,
        thread: SmsThread,
        action: ThreadBulkAction,
        target: Boolean? = null,
    ): List<SmsThread> = when (action) {
        ThreadBulkAction.MarkRead -> threads.map { if (it.id == thread.id) it.copy(unreadCount = 0) else it }
        ThreadBulkAction.Archive -> threads.map { if (it.id == thread.id) it.copy(isArchived = target ?: !thread.isArchived) else it }
        ThreadBulkAction.MarkSpam -> threads.map { if (it.id == thread.id) it.copy(isSpam = target ?: !thread.isSpam) else it }
        ThreadBulkAction.Mute -> threads.map { if (it.id == thread.id) it.copy(isMuted = target ?: !thread.isMuted) else it }
        ThreadBulkAction.Delete -> threads.filterNot { it.id == thread.id }
    }

    fun applyPin(threads: List<SmsThread>, thread: SmsThread, pinned: Boolean): List<SmsThread> = threads.map {
        if (it.id == thread.id) it.copy(isPinned = pinned, pinnedAt = if (pinned) Instant.now() else null) else it
    }

    fun visibleInFolder(thread: SmsThread, folder: ThreadFolder): Boolean = when (folder) {
        ThreadFolder.Inbox -> !thread.isSpam && !thread.isArchived
        ThreadFolder.Spam -> thread.isSpam
        ThreadFolder.Archived -> thread.isArchived && !thread.isSpam
        ThreadFolder.Pinned -> thread.isPinned && !thread.isSpam && !thread.isArchived
    }

    fun filterForFolder(threads: List<SmsThread>, folder: ThreadFolder): List<SmsThread> =
        threads.filter { visibleInFolder(it, folder) }
}
