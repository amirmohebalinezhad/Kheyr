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

    @Query("""
        SELECT t.id, t.address, t.displayName, m.body AS lastMessage, m.timestamp AS lastMessageAt,
            SUM(CASE WHEN m.direction = 'Incoming' AND m.read = 0 THEN 1 ELSE 0 END) AS unreadCount,
            COALESCE(s.isPinned, 0) AS isPinned, s.pinnedAt AS pinnedAt, COALESCE(s.isMuted, 0) AS isMuted,
            COALESCE(s.isSpam, 0) AS isSpam, COALESCE(s.isArchived, 0) AS isArchived, m.simSlot AS simSlot
        FROM threads t
        JOIN messages m ON m.id = (SELECT newest.id FROM messages newest WHERE newest.threadId = t.id ORDER BY newest.timestamp DESC, newest.id DESC LIMIT 1)
        LEFT JOIN thread_state s ON s.threadId = t.id
        WHERE COALESCE(s.isSpam, 0) = 0 AND COALESCE(s.isArchived, 0) = 0
        GROUP BY t.id
        ORDER BY COALESCE(s.isPinned, 0) DESC, s.pinnedAt DESC, m.timestamp DESC
    """)
    fun inboxThreads(): List<ThreadWithLatestMessage>

    @Query("""
        SELECT t.id, t.address, t.displayName, m.body AS lastMessage, m.timestamp AS lastMessageAt,
            SUM(CASE WHEN m.direction = 'Incoming' AND m.read = 0 THEN 1 ELSE 0 END) AS unreadCount,
            COALESCE(s.isPinned, 0) AS isPinned, s.pinnedAt AS pinnedAt, COALESCE(s.isMuted, 0) AS isMuted,
            COALESCE(s.isSpam, 0) AS isSpam, COALESCE(s.isArchived, 0) AS isArchived, m.simSlot AS simSlot
        FROM threads t
        JOIN messages m ON m.id = (SELECT newest.id FROM messages newest WHERE newest.threadId = t.id ORDER BY newest.timestamp DESC, newest.id DESC LIMIT 1)
        LEFT JOIN thread_state s ON s.threadId = t.id
        WHERE COALESCE(s.isSpam, 0) = 1
        GROUP BY t.id ORDER BY m.timestamp DESC
    """)
    fun spamThreads(): List<ThreadWithLatestMessage>

    @Query("""
        SELECT t.id, t.address, t.displayName, m.body AS lastMessage, m.timestamp AS lastMessageAt,
            SUM(CASE WHEN m.direction = 'Incoming' AND m.read = 0 THEN 1 ELSE 0 END) AS unreadCount,
            COALESCE(s.isPinned, 0) AS isPinned, s.pinnedAt AS pinnedAt, COALESCE(s.isMuted, 0) AS isMuted,
            COALESCE(s.isSpam, 0) AS isSpam, COALESCE(s.isArchived, 0) AS isArchived, m.simSlot AS simSlot
        FROM threads t
        JOIN messages m ON m.id = (SELECT newest.id FROM messages newest WHERE newest.threadId = t.id ORDER BY newest.timestamp DESC, newest.id DESC LIMIT 1)
        LEFT JOIN thread_state s ON s.threadId = t.id
        WHERE COALESCE(s.isArchived, 0) = 1 AND COALESCE(s.isSpam, 0) = 0
        GROUP BY t.id ORDER BY m.timestamp DESC
    """)
    fun archivedThreads(): List<ThreadWithLatestMessage>

    @Query("UPDATE thread_state SET isPinned = :isPinned, pinnedAt = :pinnedAt WHERE threadId = :threadId")
    fun updatePinned(threadId: Long, isPinned: Boolean, pinnedAt: Instant?)

    @Query("UPDATE thread_state SET isArchived = :isArchived WHERE threadId = :threadId")
    fun updateArchived(threadId: Long, isArchived: Boolean)

    @Query("UPDATE thread_state SET isSpam = :isSpam WHERE threadId = :threadId")
    fun updateSpam(threadId: Long, isSpam: Boolean)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    fun updateSendStatus(messageId: Long, status: MessageStatus)
}
