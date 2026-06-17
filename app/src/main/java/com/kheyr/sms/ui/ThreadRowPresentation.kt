package com.kheyr.sms.ui

import com.kheyr.sms.data.SmsThread

class ThreadRowPresentationMapper {
    fun map(thread: SmsThread, folder: ThreadFolder): ThreadRowPresentation = ThreadRowPresentation(
        title = thread.displayName.ifBlank { thread.address },
        preview = thread.lastMessage,
        unreadBadge = thread.unreadCount.takeIf { it > 0 }?.coerceAtMost(MAX_BADGE_COUNT)?.toString()?.let {
            if (thread.unreadCount > MAX_BADGE_COUNT) "$MAX_BADGE_COUNT+" else it
        },
        showPinned = thread.isPinned,
        showMuted = thread.isMuted,
        simBadge = thread.simSlot?.let { "SIM ${it + 1}" },
        showSpamBadge = folder == ThreadFolder.Spam && thread.isSpam,
    )

    private companion object { const val MAX_BADGE_COUNT = 99 }
}

data class ThreadRowPresentation(
    val title: String,
    val preview: String,
    val unreadBadge: String?,
    val showPinned: Boolean,
    val showMuted: Boolean,
    val simBadge: String?,
    val showSpamBadge: Boolean,
)

enum class ThreadFolder { Inbox, Spam, Archived, Pinned }
