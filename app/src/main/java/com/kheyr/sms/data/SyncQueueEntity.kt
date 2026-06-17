package com.kheyr.sms.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val payloadJson: String,
    val createdAt: Instant,
    val uploaded: Boolean = false,
)
