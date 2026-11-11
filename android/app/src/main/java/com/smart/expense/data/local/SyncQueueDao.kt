package com.smart.expense.data.local

import androidx.room.*
import com.smart.expense.data.local.entities.SyncQueueEntity

@Dao
interface SyncQueueDao {

    @Query("SELECT * FROM sync_queue ORDER BY queueId ASC")
    suspend fun getAllQueueItems(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToQueue(item: SyncQueueEntity): Long

    @Delete
    suspend fun removeFromQueue(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE queueId = :queueId")
    suspend fun deleteById(queueId: Long)

    @Query("DELETE FROM sync_queue")
    suspend fun clearQueue()

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getQueueCount(): Int
}

