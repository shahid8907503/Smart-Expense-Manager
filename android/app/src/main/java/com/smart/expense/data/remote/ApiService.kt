package com.smart.expense.data.remote

import com.smart.expense.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/auth/profile")
    suspend fun getProfile(): Response<AuthResponse>

    // Transactions
    @POST("api/transactions")
    suspend fun createTransaction(@Body request: CreateTransactionRequest): Response<TransactionResponse>

    @POST("api/transactions")
    suspend fun syncBatchTransactions(@Body transactions: List<CreateTransactionRequest>): Response<TransactionListResponse>

    @GET("api/transactions")
    suspend fun getTransactions(
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("search") search: String? = null
    ): Response<TransactionListResponse>

    @GET("api/transactions/{id}")
    suspend fun getTransactionById(@Path("id") id: String): Response<TransactionResponse>

    @PUT("api/transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: String,
        @Body request: CreateTransactionRequest
    ): Response<TransactionResponse>

    @DELETE("api/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String): Response<Unit>

    // Budgets
    @GET("api/budgets")
    suspend fun getBudgets(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): Response<BudgetListResponse>

    @POST("api/budgets")
    suspend fun setBudget(@Body request: SetBudgetRequest): Response<BudgetDto>

    @DELETE("api/budgets/{id}")
    suspend fun deleteBudget(@Path("id") id: String): Response<Unit>

    // Analytics
    @GET("api/analytics/summary")
    suspend fun getSummary(): Response<DashboardSummaryResponse>

    @GET("api/analytics/categories")
    suspend fun getCategoryBreakdown(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): Response<CategoryBreakdownResponse>

    @GET("api/analytics/monthly")
    suspend fun getMonthlyTrends(): Response<MonthlyTrendsResponse>

    // Voice & NLP
    @POST("api/voice/parse")
    suspend fun parseVoiceInput(@Body request: VoiceParseRequest): Response<VoiceParseResponse>

    @POST("api/voice/query")
    suspend fun queryVoiceAssistant(@Body request: VoiceQueryRequest): Response<VoiceQueryResponse>
}

