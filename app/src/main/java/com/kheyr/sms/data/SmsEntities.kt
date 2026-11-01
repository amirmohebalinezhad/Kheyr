package com.kheyr.sms.data

import androidx.room.ColumnInfo
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
    /**
     * Rule-derived category ("financial", "otp", "advertising"...), null until a rule resolves one.
     *
     * Per message rather than per thread on purpose: a commercial sender's thread carries delivery
     * notices and promotions at once, and the thread-level [SyncSpamMetadataEntity] cannot say which
     * message was which.
     */
    val categoryId: String? = null,
    /**
     * What the message scored on arrival, kept so a later reclassification can be explained and
     * compared. Distinct from [SyncSpamMetadataEntity.spamScore], which is a thread-level float.
     */
    @ColumnInfo(defaultValue = "0") val spamScore: Int = 0,
)

/**
 * A message category, built in or created by the user. Built-in categories are the ones the rule
 * pack resolves; user categories are created from settings or the assignment sheet.
 *
 * [deletedAt] is a tombstone rather than a hard delete so the row can sync as user data.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(defaultValue = "0") val isBuiltIn: Boolean = false,
    val colorKey: String? = null,
    @ColumnInfo(defaultValue = "0") val sortOrder: Int = 0,
    val createdAt: Instant,
    val deletedAt: Instant? = null,
)

/**
 * A user's category assignment for one sender, which outranks any rule-derived category.
 *
 * [senderKey] must be `PhoneNumberNormalizer.matchKey(address)`, the same keying blocked senders
 * use: the address on a thread and the address in the PDU are formatted differently far more often
 * than not, and an exact string match would quietly fail to apply the assignment.
 */
@Entity(tableName = "sender_category")
data class SenderCategoryEntity(
    @PrimaryKey val senderKey: String,
    val categoryId: String,
    val source: String,
    val assignedAt: Instant,
)

@Entity(tableName = "thread_state")
data class ThreadStateEntity(
    @PrimaryKey val threadId: Long,
    val isPinned: Boolean = false,
    val pinnedAt: Instant? = null,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val isSpam: Boolean = false,
    // Records that the user explicitly said "Not spam" for this thread; the automatic spam
    // classifier must never override that decision (B-23).
    val userNotSpam: Boolean = false,
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
