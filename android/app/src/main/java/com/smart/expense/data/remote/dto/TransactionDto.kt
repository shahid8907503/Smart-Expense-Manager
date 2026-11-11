package com.smart.expense.data.remote.dto

data class CreateTransactionRequest(
    val amount: Double,
    val type: String,
    val category: String,
    val description: String,
    val transactionDate: String? = null,
    val paymentMethod: String = "Cash",
    val source: String = "manual",
    val rawVoiceText: String? = null
)

data class TransactionResponse(
    val success: Boolean,
    val message: String? = null,
    val data: TransactionDto? = null
)

data class TransactionListResponse(
    val success: Boolean,
    val count: Int = 0,
    val total: Int = 0,
    val data: List<TransactionDto> = emptyList()
)

data class TransactionDto(
    val _id: String,
    val userId: String,
    val amount: Double,
    val type: String,
    val category: String,
    val description: String,
    val transactionDate: String,
    val paymentMethod: String = "Cash",
    val source: String = "manual",
    val syncStatus: String = "synced",
    val rawVoiceText: String? = null,
    val createdAt: String? = null
)

