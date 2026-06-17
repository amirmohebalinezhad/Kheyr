package com.kheyr.sms.importing

data class SmsImportProgress(val importedMessages: Int, val totalMessages: Int, val currentThreadAddress: String? = null) {
    val isComplete: Boolean get() = totalMessages >= 0 && importedMessages >= totalMessages
    val percent: Int get() = if (totalMessages <= 0) 0 else ((importedMessages.coerceAtMost(totalMessages) * 100) / totalMessages)
}
