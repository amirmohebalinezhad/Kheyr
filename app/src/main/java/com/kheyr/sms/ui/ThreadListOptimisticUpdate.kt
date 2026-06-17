package com.kheyr.sms.ui

import com.kheyr.sms.data.SmsThread
import com.kheyr.sms.thread.ThreadBulkAction
import java.time.Instant

object ThreadListOptimisticUpdate {
    fun applyAction(threads: List<SmsThread>, thread: SmsThread, action: ThreadBulkAction): List<SmsThread> = when (action) {
        ThreadBulkAction.MarkRead -> threads.map { if (it.id == thread.id) it.copy(unreadCount = 0) else it }
        ThreadBulkAction.Archive -> threads.map { if (it.id == thread.id) it.copy(isArchived = !thread.isArchived) else it }
        ThreadBulkAction.MarkSpam -> threads.map { if (it.id == thread.id) it.copy(isSpam = !thread.isSpam) else it }
        ThreadBulkAction.Mute -> threads.map { if (it.id == thread.id) it.copy(isMuted = !thread.isMuted) else it }
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
