package com.kheyr.sms.sync

import com.kheyr.sms.data.MessageDirection
import com.kheyr.sms.data.MessageStatus
import com.kheyr.sms.data.SmsMessage
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class InitialBackfillPlannerTest {
    @Test fun backfillPlansCurrentMessagesAndMarksDeletedHistoryToSkip() {
        val enabledAt = Instant.parse("2026-01-02T00:00:00Z")
        val plan = InitialBackfillPlanner().plan(
            messages = listOf(sms(2, "2026-01-02T00:00:00Z"), sms(1, "2026-01-01T00:00:00Z")),
            deletedBeforeSyncMessageIds = setOf(2),
            enabledAt = enabledAt,
        )

        assertEquals(listOf(1L, 2L), plan.map { it.message.id })
        assertEquals(listOf(false, true), plan.map { it.locallyDeletedBeforeSync })
        assertEquals(listOf(enabledAt, enabledAt), plan.map { it.createdAt })
    }

    private fun sms(id: Long, at: String) = SmsMessage(
        id = id,
        threadId = 7,
        address = "+15551234567",
        body = "body $id",
        timestamp = Instant.parse(at),
        direction = MessageDirection.Incoming,
        status = MessageStatus.Received,
    )
}
