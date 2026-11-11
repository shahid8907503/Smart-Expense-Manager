package com.smart.expense.data.remote.dto

data class DashboardSummaryResponse(
    val success: Boolean,
    val data: DashboardSummaryDto
)

data class DashboardSummaryDto(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val todaySpending: Double,
    val thisMonthSpending: Double,
    val monthlyBudgetTotal: Double,
    val remainingBudget: Double,
    val currency: String = "₹"
)

data class CategoryBreakdownResponse(
    val success: Boolean,
    val totalExpense: Double,
    val month: Int,
    val year: Int,
    val data: List<CategorySpendDto>
)

data class CategorySpendDto(
    val category: String,
    val amount: Double,
    val percentage: Double
)

data class MonthlyTrendsResponse(
    val success: Boolean,
    val data: List<MonthlyTrendBucketDto>,
    val insight: String? = null
)

data class MonthlyTrendBucketDto(
    val monthName: String,
    val month: Int,
    val year: Int,
    val income: Double,
    val expense: Double
)

