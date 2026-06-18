package com.kheyr.sms.data

import android.net.Uri
import java.time.Instant

data class SmsThread(
    val id: Long,
    val address: String,
    val displayName: String,
    val lastMessage: String,
    val lastMessageAt: Instant,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val pinnedAt: Instant? = null,
    val isMuted: Boolean = false,
    val isSpam: Boolean = false,
    val isArchived: Boolean = false,
    val simSlot: Int? = null,
    val contactPhotoUri: Uri? = null,
)

data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Instant,
    val direction: MessageDirection,
    val status: MessageStatus,
    val simSlot: Int? = null,
    val telephonyId: Long? = null,
    val isSpam: Boolean = false,
)

enum class MessageDirection { Incoming, Outgoing }
enum class MessageStatus { Received, Sending, Sent, Delivered, Failed }
