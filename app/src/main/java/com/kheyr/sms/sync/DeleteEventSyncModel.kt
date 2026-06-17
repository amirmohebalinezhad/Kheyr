package com.kheyr.sms.sync

data class DeleteEventSyncModel(val localMessageId: Long, val serverMessageId: String?, val occurredAtMillis: Long) {
    val canSync: Boolean get() = serverMessageId != null
}
