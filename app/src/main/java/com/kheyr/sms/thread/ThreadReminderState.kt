package com.kheyr.sms.thread

data class ThreadReminderState(val threadId: Long, val note: String, val dueAtMillis: Long, val triggered: Boolean = false)
