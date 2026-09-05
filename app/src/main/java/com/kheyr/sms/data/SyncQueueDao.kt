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

    /**
     * Drops every queued event. Used on sign-out: the queue holds plaintext bodies belonging to the
     * account that queued them, and leaving it in place uploads one account's messages under
     * whichever account signs in next.
     */
    @Query("DELETE FROM sync_queue")
    fun deleteAll()
}
