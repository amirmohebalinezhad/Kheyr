package com.kheyr.sms.receiver

/**
 * The conversation the user currently has on screen, if any.
 *
 * The receive pipeline runs on a background broadcast thread while the UI writes this from the main
 * thread, so the field is @Volatile: a plain field could leave the receiver reading a stale thread id
 * and posting a notification for the chat the user is already looking at.
 */
object ActiveConversation {
    @Volatile
    private var openThreadId: Long? = null

    fun setOpen(threadId: Long?) {
        openThreadId = threadId
    }

    fun isOpen(threadId: Long): Boolean = openThreadId == threadId
}

/** Shared derivation of a thread's notification id, so the UI can dismiss what the receiver posted. */
object IncomingSmsNotifications {
    fun notificationId(threadId: Long): Int = (threadId xor (threadId ushr 32)).toInt()
}
