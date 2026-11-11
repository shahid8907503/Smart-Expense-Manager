package com.smart.expense.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String, // UUID or MongoDB ObjectId
    val userId: String,
    val amount: Double,
    val type: String, // "expense" or "income"
    val category: String, // Food, Travel, Shopping, Bills, Health, Education, Entertainment, Rent, Other
    val description: String,
    val transactionDate: Long, // timestamp epoch ms
    val paymentMethod: String = "Cash", // Cash, UPI, Debit Card, Credit Card, Net Banking
    val source: String = "manual", // "voice" or "manual"
    val syncStatus: String = "SYNCED", // "SYNCED", "PENDING_SYNC"
    val rawVoiceText: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val paymentType: String = "EXTERNAL" // "EXTERNAL" (Merchant/Expenses) or "INTERNAL" (Self Transfer/Bank to Wallet)
)

