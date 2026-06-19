package com.kheyr.sms.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.time.Instant

@Dao
interface SyncQueueDao {
    @Insert
    fun insert(entity: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue WHERE uploaded = 0 ORDER BY createdAt ASC, id ASC LIMIT :limit")
    fun pending(limit: Int): List<SyncQueueEntity>

    @Query("UPDATE sync_queue SET uploaded = 1 WHERE id IN (:ids)")
    fun markUploaded(ids: List<Long>)

    @Query("DELETE FROM sync_queue WHERE id IN (:ids)")
    fun deleteByIds(ids: List<Long>)
}
