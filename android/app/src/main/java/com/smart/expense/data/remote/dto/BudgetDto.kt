package com.smart.expense.data.remote.dto

data class SetBudgetRequest(
    val category: String,
    val monthlyLimit: Double,
    val month: Int,
    val year: Int
)

data class BudgetListResponse(
    val success: Boolean,
    val month: Int,
    val year: Int,
    val data: List<BudgetDto> = emptyList()
)

data class BudgetDto(
    val _id: String,
    val userId: String,
    val category: String,
    val monthlyLimit: Double,
    val month: Int,
    val year: Int,
    val alertThresholdPercent: Int = 80,
    val spent: Double = 0.0,
    val remaining: Double = 0.0,
    val percentageUsed: Double = 0.0,
    val status: String = "HEALTHY" // HEALTHY, WARNING, EXCEEDED
)

