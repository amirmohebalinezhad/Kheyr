package com.kheyr.sms.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "threads")
data class SmsThreadEntity(
    @PrimaryKey val id: Long,
    val address: String,
    val displayName: String = address,
    val createdAt: Instant,
)

@Entity(
    tableName = "messages",
    indices = [Index("threadId"), Index(value = ["telephonyId"], unique = true)]
)
data class SmsMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val telephonyId: Long? = null,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Instant,
    val direction: MessageDirection,
    val status: MessageStatus,
    val read: Boolean,
    val simSlot: Int? = null,
)

@Entity(tableName = "thread_state")
data class ThreadStateEntity(
    @PrimaryKey val threadId: Long,
    val isPinned: Boolean = false,
    val pinnedAt: Instant? = null,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val isSpam: Boolean = false,
)

@Entity(tableName = "sync_spam_metadata")
data class SyncSpamMetadataEntity(
    @PrimaryKey val threadId: Long,
    val lastSyncedTelephonyMessageId: Long? = null,
    val lastSyncedAt: Instant? = null,
    val spamScore: Double = 0.0,
    val spamReason: String? = null,
    val markedSpamAt: Instant? = null,
)

data class ThreadWithLatestMessage(
    val id: Long,
    val address: String,
    val displayName: String,
    val lastMessage: String,
    val lastMessageAt: Instant,
    val unreadCount: Int,
    val isPinned: Boolean,
    val pinnedAt: Instant?,
    val isMuted: Boolean,
    val isSpam: Boolean,
    val isArchived: Boolean,
    val simSlot: Int?,
)
