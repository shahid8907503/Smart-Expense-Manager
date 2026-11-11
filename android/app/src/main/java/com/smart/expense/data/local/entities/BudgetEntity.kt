package com.smart.expense.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val category: String,
    val monthlyLimit: Double,
    val month: Int, // 1 to 12
    val year: Int,
    val alertThresholdPercent: Int = 80,
    val syncStatus: String = "SYNCED",
    val updatedAt: Long = System.currentTimeMillis(),
    val periodType: String = "MONTHLY" // "WEEKLY", "MONTHLY", "ANNUAL"
)

