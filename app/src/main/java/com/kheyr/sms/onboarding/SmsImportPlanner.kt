package com.kheyr.sms.onboarding

data class SmsImportPlanner(val batchSize: Int = 250) {
    fun batches(totalMessages: Int): Int = if (totalMessages == 0) 0 else ((totalMessages - 1) / batchSize) + 1
}
