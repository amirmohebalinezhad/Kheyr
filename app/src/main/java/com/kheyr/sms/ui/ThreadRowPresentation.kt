package com.kheyr.sms.ui

import com.kheyr.sms.data.SmsThread
import com.kheyr.sms.telephony.SimBadgeResolver
import com.kheyr.sms.telephony.SimCard

class ThreadRowPresentationMapper {
    fun map(thread: SmsThread, folder: ThreadFolder, activeSims: List<SimCard> = emptyList()): ThreadRowPresentation = ThreadRowPresentation(
        title = thread.displayName.ifBlank { thread.address },
        preview = thread.lastMessage,
        unreadBadge = thread.unreadCount.takeIf { it > 0 }?.coerceAtMost(MAX_BADGE_COUNT)?.toString()?.let {
            if (thread.unreadCount > MAX_BADGE_COUNT) "$MAX_BADGE_COUNT+" else it
        },
        showPinned = thread.isPinned,
        showMuted = thread.isMuted,
        // Only worth showing when there is a choice to disambiguate. On a single-SIM phone the badge
        // would sit on every row saying the same thing, stealing width from the message preview.
        simBadge = if (activeSims.size > 1) SimBadgeResolver.badge(thread.simSlot, activeSims) else null,
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
