package com.kheyr.sms.sync

data class ThreadStateSyncPayload(val threadId: Long, val spam: Boolean, val archived: Boolean, val pinnedAtMillis: Long?, val unreadCount: Int)
