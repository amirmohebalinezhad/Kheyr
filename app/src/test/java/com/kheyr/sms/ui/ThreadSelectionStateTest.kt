package com.kheyr.sms.ui

import com.kheyr.sms.thread.ThreadBulkAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadSelectionStateTest {
    @Test fun longPressSelectsThreadAndEntersSelectionMode() {
        val state = ThreadSelectionState().select(42L)

        assertTrue(state.isSelectionMode)
        assertEquals(setOf(42L), state.selectedThreadIds)
        assertTrue(state.isSelected(42L))
    }

    @Test fun toggleAddsAndRemovesThreadsWhileSelecting() {
        val state = ThreadSelectionState()
            .select(1L)
            .toggle(2L)
            .toggle(1L)

        assertTrue(state.isSelectionMode)
        assertEquals(setOf(2L), state.selectedThreadIds)
        assertFalse(state.isSelected(1L))
    }

    @Test fun clearExitsSelectionMode() {
        val state = ThreadSelectionState().select(1L).clear()

        assertFalse(state.isSelectionMode)
        assertEquals(emptySet<Long>(), state.selectedThreadIds)
    }

    @Test fun bulkActionRequestContainsAllSelectedThreadIds() {
        val request = ThreadSelectionState()
            .select(1L)
            .toggle(2L)
            .request(ThreadBulkAction.Archive)

        assertTrue(request.isRunnable)
        assertEquals(setOf(1L, 2L), request.threadIds)
        assertEquals(ThreadBulkAction.Archive, request.action)
    }
}
