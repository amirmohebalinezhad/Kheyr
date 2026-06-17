package com.kheyr.sms.sync

import com.kheyr.sms.data.SmsMessage
import java.time.Instant

/** Plans first sync so current messages upload but history deleted before opt-in is skipped. */
class InitialBackfillPlanner {
    fun plan(messages: List<SmsMessage>, deletedBeforeSyncMessageIds: Set<Long>, enabledAt: Instant): List<InitialBackfillSyncRecord> =
        messages.sortedBy { it.timestamp }.mapIndexed { index, message ->
            InitialBackfillSyncRecord(
                queueId = index + 1L,
                createdAt = enabledAt,
                message = message,
                locallyDeletedBeforeSync = message.id in deletedBeforeSyncMessageIds,
            )
        }
}
