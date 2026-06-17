package com.kheyr.sms.ui

import com.kheyr.sms.data.MessageStatus

data class RetryableSendUiModel(
    val messageId: Long,
    val statusLabel: String,
    val showRetry: Boolean,
    val retryLabel: String = "Retry",
)

object RetryableSendUiMapper {
    fun map(messageId: Long, status: MessageStatus): RetryableSendUiModel = RetryableSendUiModel(
        messageId = messageId,
        statusLabel = status.name.lowercase().replaceFirstChar { it.uppercase() },
        showRetry = status == MessageStatus.Failed,
    )
}
