package com.kheyr.sms.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import java.time.Instant

@Dao
interface SmsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertThread(thread: SmsThreadEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertThreadState(state: ThreadStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSyncSpamMetadata(metadata: SyncSpamMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertMessage(message: SmsMessageEntity): Long

    @Transaction
    fun insertIncomingSms(message: SmsMessageEntity): Long = insertSms(message.copy(direction = MessageDirection.Incoming, status = MessageStatus.Received))

    @Transaction
    fun insertOutgoingSms(message: SmsMessageEntity): Long = insertSms(message.copy(direction = MessageDirection.Outgoing))

    @Transaction
    fun insertSms(message: SmsMessageEntity): Long {
        upsertThread(SmsThreadEntity(message.threadId, message.address, message.address, message.timestamp))
        insertThreadState(ThreadStateEntity(message.threadId))
        return insertMessage(message)
    }

    @Transaction
    fun insertSmsBatch(messages: List<SmsMessageEntity>) {
        messages.forEach { insertSms(it) }
    }

    @Query("SELECT COALESCE(MAX(telephonyId), 0) FROM messages WHERE telephonyId IS NOT NULL")
    fun latestSyncedTelephonyId(): Long

    @Query("""
        SELECT t.id, t.address, t.displayName, m.body AS lastMessage, m.timestamp AS lastMessageAt,
            (SELECT COUNT(*) FROM messages unread
                WHERE unread.threadId = t.id AND unread.direction = 'Incoming' AND unread.read = 0) AS unreadCount,
            COALESCE(s.isPinned, 0) AS isPinned, s.pinnedAt AS pinnedAt, COALESCE(s.isMuted, 0) AS isMuted,
            COALESCE(s.isSpam, 0) AS isSpam, COALESCE(s.isArchived, 0) AS isArchived, m.simSlot AS simSlot
        FROM threads t
        JOIN messages m ON m.id = (SELECT newest.id FROM messages newest WHERE newest.threadId = t.id ORDER BY newest.timestamp DESC, newest.id DESC LIMIT 1)
        LEFT JOIN thread_state s ON s.threadId = t.id
        WHERE COALESCE(s.isSpam, 0) = 0 AND COALESCE(s.isArchived, 0) = 0
        ORDER BY COALESCE(s.isPinned, 0) DESC, s.pinnedAt DESC, m.timestamp DESC
    """)
    fun inboxThreads(): List<ThreadWithLatestMessage>

    @Query("""
        SELECT t.id, t.address, t.displayName, m.body AS lastMessage, m.timestamp AS lastMessageAt,
            (SELECT COUNT(*) FROM messages unread
                WHERE unread.threadId = t.id AND unread.direction = 'Incoming' AND unread.read = 0) AS unreadCount,
            COALESCE(s.isPinned, 0) AS isPinned, s.pinnedAt AS pinnedAt, COALESCE(s.isMuted, 0) AS isMuted,
            COALESCE(s.isSpam, 0) AS isSpam, COALESCE(s.isArchived, 0) AS isArchived, m.simSlot AS simSlot
        FROM threads t
        JOIN messages m ON m.id = (SELECT newest.id FROM messages newest WHERE newest.threadId = t.id ORDER BY newest.timestamp DESC, newest.id DESC LIMIT 1)
        LEFT JOIN thread_state s ON s.threadId = t.id
        WHERE COALESCE(s.isSpam, 0) = 1
        ORDER BY m.timestamp DESC
    """)
    fun spamThreads(): List<ThreadWithLatestMessage>

    @Query("""
        SELECT t.id, t.address, t.displayName, m.body AS lastMessage, m.timestamp AS lastMessageAt,
            (SELECT COUNT(*) FROM messages unread
                WHERE unread.threadId = t.id AND unread.direction = 'Incoming' AND unread.read = 0) AS unreadCount,
            COALESCE(s.isPinned, 0) AS isPinned, s.pinnedAt AS pinnedAt, COALESCE(s.isMuted, 0) AS isMuted,
            COALESCE(s.isSpam, 0) AS isSpam, COALESCE(s.isArchived, 0) AS isArchived, m.simSlot AS simSlot
        FROM threads t
        JOIN messages m ON m.id = (SELECT newest.id FROM messages newest WHERE newest.threadId = t.id ORDER BY newest.timestamp DESC, newest.id DESC LIMIT 1)
        LEFT JOIN thread_state s ON s.threadId = t.id
        WHERE COALESCE(s.isArchived, 0) = 1 AND COALESCE(s.isSpam, 0) = 0
        ORDER BY m.timestamp DESC
    """)
    fun archivedThreads(): List<ThreadWithLatestMessage>

    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY timestamp ASC, id ASC")
    fun messagesForThread(threadId: Long): List<SmsMessageEntity>

    @Query("SELECT * FROM messages WHERE body LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' ORDER BY timestamp DESC, id DESC")
    fun searchMessages(query: String): List<SmsMessageEntity>

    @Query("SELECT * FROM messages WHERE status = 'Failed' ORDER BY timestamp ASC, id ASC")
    fun failedOutgoingMessages(): List<SmsMessageEntity>

    @Transaction
    fun ensureThreadState(threadId: Long) {
        insertThreadState(ThreadStateEntity(threadId))
    }

    @Transaction
    fun markThreadRead(threadId: Long) {
        ensureThreadState(threadId)
        markThreadReadMessages(threadId)
    }

    @Query("UPDATE messages SET read = 1 WHERE threadId = :threadId AND direction = 'Incoming'")
    fun markThreadReadMessages(threadId: Long)

    @Transaction
    fun updatePinned(threadId: Long, isPinned: Boolean, pinnedAt: Instant?) {
        ensureThreadState(threadId)
        updatePinnedState(threadId, isPinned, pinnedAt)
    }

    @Query("UPDATE thread_state SET isPinned = :isPinned, pinnedAt = :pinnedAt WHERE threadId = :threadId")
    fun updatePinnedState(threadId: Long, isPinned: Boolean, pinnedAt: Instant?)

    @Transaction
    fun updateArchived(threadId: Long, isArchived: Boolean) {
        ensureThreadState(threadId)
        updateArchivedState(threadId, isArchived)
    }

    @Query("UPDATE thread_state SET isArchived = :isArchived WHERE threadId = :threadId")
    fun updateArchivedState(threadId: Long, isArchived: Boolean)

    @Transaction
    fun updateSpam(threadId: Long, isSpam: Boolean) {
        ensureThreadState(threadId)
        updateSpamState(threadId, isSpam)
    }

    @Query("UPDATE thread_state SET isSpam = :isSpam WHERE threadId = :threadId")
    fun updateSpamState(threadId: Long, isSpam: Boolean)

    @Transaction
    fun updateMuted(threadId: Long, isMuted: Boolean) {
        ensureThreadState(threadId)
        updateMutedState(threadId, isMuted)
    }

    @Query("UPDATE thread_state SET isMuted = :isMuted WHERE threadId = :threadId")
    fun updateMutedState(threadId: Long, isMuted: Boolean)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    fun updateSendStatus(messageId: Long, status: MessageStatus)

    @Query("UPDATE messages SET status = :status WHERE telephonyId = :telephonyId")
    fun updateSendStatusByTelephonyId(telephonyId: Long, status: MessageStatus)

    @Query("""
        SELECT telephonyId FROM messages
        WHERE telephonyId IS NOT NULL AND direction = 'Outgoing' AND status != 'Delivered'
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    fun recentOutgoingTelephonyIds(limit: Int): List<Long>

    @Query("SELECT * FROM messages WHERE telephonyId = :telephonyId LIMIT 1")
    fun messageByTelephonyId(telephonyId: Long): SmsMessageEntity?

    @Query("""
        UPDATE messages
        SET threadId = :threadId, address = :address, body = :body, timestamp = :timestamp,
            direction = :direction, status = :status, read = :read, simSlot = :simSlot
        WHERE telephonyId = :telephonyId
    """)
    fun updateTelephonyMessage(
        telephonyId: Long,
        threadId: Long,
        address: String,
        body: String,
        timestamp: Instant,
        direction: MessageDirection,
        status: MessageStatus,
        read: Boolean,
        simSlot: Int?,
    ): Int

    @Transaction
    fun upsertTelephonyMessage(message: SmsMessageEntity) {
        val telephonyId = message.telephonyId
        if (telephonyId == null) {
            insertSms(message)
            return
        }
        val existing = messageByTelephonyId(telephonyId)
        if (existing == null) {
            insertSms(message)
        } else {
            val direction = if (
                existing.direction == MessageDirection.Outgoing &&
                message.direction == MessageDirection.Incoming
            ) {
                MessageDirection.Outgoing
            } else {
                message.direction
            }
            val status = if (direction == MessageDirection.Outgoing) {
                preferOutgoingSendStatus(existing.status, message.status)
            } else {
                message.status
            }
            updateTelephonyMessage(
                telephonyId = telephonyId,
                threadId = message.threadId,
                address = message.address,
                body = message.body,
                timestamp = message.timestamp,
                direction = direction,
                status = status,
                read = message.read,
                simSlot = message.simSlot,
            )
            upsertThread(SmsThreadEntity(message.threadId, message.address, message.address, message.timestamp))
        }
    }

    @Transaction
    fun upsertTelephonyMessageBatch(messages: List<SmsMessageEntity>) {
        messages.forEach(::upsertTelephonyMessage)
    }

    @Query("SELECT telephonyId FROM messages WHERE telephonyId IS NOT NULL")
    fun syncedTelephonyIds(): List<Long>

    @Query("""
        SELECT t.id, t.address, t.displayName, m.body AS lastMessage, m.timestamp AS lastMessageAt,
            (SELECT COUNT(*) FROM messages unread
                WHERE unread.threadId = t.id AND unread.direction = 'Incoming' AND unread.read = 0) AS unreadCount,
            COALESCE(s.isPinned, 0) AS isPinned, s.pinnedAt AS pinnedAt, COALESCE(s.isMuted, 0) AS isMuted,
            COALESCE(s.isSpam, 0) AS isSpam, COALESCE(s.isArchived, 0) AS isArchived, m.simSlot AS simSlot
        FROM threads t
        JOIN messages m ON m.id = (SELECT newest.id FROM messages newest WHERE newest.threadId = t.id ORDER BY newest.timestamp DESC, newest.id DESC LIMIT 1)
        LEFT JOIN thread_state s ON s.threadId = t.id
        WHERE COALESCE(s.isPinned, 0) = 1 AND COALESCE(s.isSpam, 0) = 0 AND COALESCE(s.isArchived, 0) = 0
        ORDER BY s.pinnedAt DESC, m.timestamp DESC
    """)
    fun pinnedThreads(): List<ThreadWithLatestMessage>

    @Query("DELETE FROM messages WHERE threadId = :threadId")
    fun deleteThreadMessages(threadId: Long)

    @Query("DELETE FROM messages WHERE id IN (:ids)")
    fun deleteMessagesByIds(ids: List<Long>)

    @Query("SELECT COALESCE(s.isMuted, 0) FROM thread_state s WHERE s.threadId = :threadId")
    fun isThreadMuted(threadId: Long): Boolean?

    @Query("SELECT COALESCE(s.isSpam, 0) FROM thread_state s WHERE s.threadId = :threadId")
    fun isThreadSpam(threadId: Long): Boolean?

    @Query("""
        SELECT threadId FROM messages
        WHERE address = :address AND body = :body AND direction = 'Outgoing'
        AND timestamp >= :since
        ORDER BY timestamp DESC LIMIT 1
    """)
    fun recentOutgoingThreadId(address: String, body: String, since: Instant): Long?

    @Query("SELECT * FROM sync_spam_metadata WHERE threadId = :threadId")
    fun syncSpamMetadata(threadId: Long): SyncSpamMetadataEntity?
}

private fun preferOutgoingSendStatus(local: MessageStatus, synced: MessageStatus): MessageStatus {
    if (local == MessageStatus.Sent || local == MessageStatus.Delivered) {
        return when (synced) {
            MessageStatus.Failed, MessageStatus.Sending -> local
            else -> synced
        }
    }
    return synced
}
