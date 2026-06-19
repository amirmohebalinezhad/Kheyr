package com.kheyr.sms.ui

import com.kheyr.sms.data.SmsThread
import com.kheyr.sms.thread.ThreadBulkAction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ThreadListOptimisticUpdateTest {
    private fun thread(id: Long, isMuted: Boolean = false, isArchived: Boolean = false, isSpam: Boolean = false) = SmsThread(
        id = id,
        address = "0912000000$id",
        displayName = "Thread $id",
        lastMessage = "hi",
        lastMessageAt = Instant.now(),
        isMuted = isMuted,
        isArchived = isArchived,
        isSpam = isSpam,
    )

    @Test
    fun applyActionWithoutTargetTogglesFromThreadState() {
        val muted = thread(id = 1, isMuted = true)
        val result = ThreadListOptimisticUpdate.applyAction(listOf(muted), muted, ThreadBulkAction.Mute)
        assertEquals(false, result.first().isMuted)
    }

    @Test
    fun applyActionWithTargetForcesConsistentStateAcrossMixedSelection() {
        val muted = thread(id = 1, isMuted = true)
        val unmuted = thread(id = 2, isMuted = false)
        var threads = listOf(muted, unmuted)
        // A mixed selection muted via the toolbar should end up all muted, not flip the already-muted one.
        threads = ThreadListOptimisticUpdate.applyAction(threads, muted, ThreadBulkAction.Mute, target = true)
        threads = ThreadListOptimisticUpdate.applyAction(threads, unmuted, ThreadBulkAction.Mute, target = true)
        assertEquals(listOf(true, true), threads.map { it.isMuted })
    }
}
