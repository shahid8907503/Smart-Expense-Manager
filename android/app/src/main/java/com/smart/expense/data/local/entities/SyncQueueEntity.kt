package com.smart.expense.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val queueId: Long = 0,
    val entityType: String, // "TRANSACTION" or "BUDGET"
    val operation: String, // "CREATE", "UPDATE", "DELETE"
    val entityId: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

