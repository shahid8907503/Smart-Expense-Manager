package com.smart.expense.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.smart.expense.data.local.AppDatabase
import com.smart.expense.data.local.entities.BudgetEntity
import com.smart.expense.data.local.entities.SyncQueueEntity
import com.smart.expense.data.local.entities.TransactionEntity
import com.smart.expense.data.remote.NetworkClient
import com.smart.expense.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ExpenseRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val transactionDao = db.transactionDao()
    private val budgetDao = db.budgetDao()
    private val syncQueueDao = db.syncQueueDao()
    private val gson = Gson()

    fun getAllTransactionsFlow(userId: String): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactionsFlow(userId)

    fun getRecentTransactionsFlow(userId: String, limit: Int = 5): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactionsFlow(userId, limit)

    fun getBudgetsFlow(userId: String, month: Int, year: Int): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsFlow(userId, month, year)

    suspend fun addTransaction(
        userId: String,
        amount: Double,
        type: String,
        category: String,
        description: String,
        timestamp: Long = System.currentTimeMillis(),
        paymentMethod: String = "Cash",
        paymentType: String = "EXTERNAL",
        source: String = "manual",
        rawVoiceText: String? = null
    ): Result<TransactionEntity> = withContext(Dispatchers.IO) {
        val txId = "tx_${UUID.randomUUID()}"
        val entity = TransactionEntity(
            id = txId,
            userId = userId,
            amount = amount,
            type = type,
            category = category,
            description = description,
            transactionDate = timestamp,
            paymentMethod = paymentMethod,
            source = source,
            syncStatus = "PENDING_SYNC",
            rawVoiceText = rawVoiceText,
            createdAt = System.currentTimeMillis(),
            paymentType = paymentType
        )

        // 1. Immediately save to Room Local DB (offline-first guarantee)
        transactionDao.insertTransaction(entity)

        // 2. Try online sync with backend
        try {
            val api = NetworkClient.getApiService(context)
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val request = CreateTransactionRequest(
                amount = amount,
                type = type,
                category = category,
                description = description,
                transactionDate = isoFormat.format(Date(timestamp)),
                paymentMethod = paymentMethod,
                source = source,
                rawVoiceText = rawVoiceText
            )
            val response = api.createTransaction(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val updated = entity.copy(syncStatus = "SYNCED")
                transactionDao.updateTransaction(updated)
                return@withContext Result.success(updated)
            } else {
                queuePendingSync("TRANSACTION", "CREATE", txId, gson.toJson(request))
                return@withContext Result.success(entity)
            }
        } catch (e: Exception) {
            Log.w("ExpenseRepo", "Saved offline, network error: ${e.message}")
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val request = CreateTransactionRequest(
                amount = amount,
                type = type,
                category = category,
                description = description,
                transactionDate = isoFormat.format(Date(timestamp)),
                paymentMethod = paymentMethod,
                source = source,
                rawVoiceText = rawVoiceText
            )
            queuePendingSync("TRANSACTION", "CREATE", txId, gson.toJson(request))
            return@withContext Result.success(entity)
        }
    }

    suspend fun updateTransaction(transaction: TransactionEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val pendingTx = transaction.copy(syncStatus = "PENDING_SYNC")
        transactionDao.updateTransaction(pendingTx)

        try {
            val api = NetworkClient.getApiService(context)
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val request = CreateTransactionRequest(
                amount = transaction.amount,
                type = transaction.type,
                category = transaction.category,
                description = transaction.description,
                transactionDate = isoFormat.format(Date(transaction.transactionDate)),
                paymentMethod = transaction.paymentMethod,
                source = transaction.source
            )
            val response = api.updateTransaction(transaction.id, request)
            if (response.isSuccessful) {
                transactionDao.updateTransaction(transaction.copy(syncStatus = "SYNCED"))
            } else {
                queuePendingSync("TRANSACTION", "UPDATE", transaction.id, gson.toJson(request))
            }
        } catch (e: Exception) {
            queuePendingSync("TRANSACTION", "UPDATE", transaction.id, gson.toJson(transaction))
        }
        Result.success(Unit)
    }

    suspend fun deleteTransaction(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        transactionDao.deleteById(id)
        try {
            val api = NetworkClient.getApiService(context)
            api.deleteTransaction(id)
        } catch (e: Exception) {
            queuePendingSync("TRANSACTION", "DELETE", id, "")
        }
        Result.success(Unit)
    }

    suspend fun setBudget(
        userId: String,
        category: String,
        monthlyLimit: Double,
        month: Int,
        year: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val bgtId = "bgt_${category}_${month}_$year"
        val entity = BudgetEntity(
            id = bgtId,
            userId = userId,
            category = category,
            monthlyLimit = monthlyLimit,
            month = month,
            year = year,
            syncStatus = "PENDING_SYNC"
        )
        budgetDao.insertBudget(entity)

        try {
            val api = NetworkClient.getApiService(context)
            val res = api.setBudget(SetBudgetRequest(category, monthlyLimit, month, year))
            if (res.isSuccessful) {
                budgetDao.insertBudget(entity.copy(syncStatus = "SYNCED"))
            }
        } catch (e: Exception) {
            queuePendingSync("BUDGET", "SET", bgtId, gson.toJson(entity))
        }
        Result.success(Unit)
    }

    suspend fun syncPendingTransactions(): Int = withContext(Dispatchers.IO) {
        val queue = syncQueueDao.getAllQueueItems()
        if (queue.isEmpty()) return@withContext 0

        var syncedCount = 0
        val api = NetworkClient.getApiService(context)

        for (item in queue) {
            try {
                if (item.entityType == "TRANSACTION" && item.operation == "CREATE") {
                    val req = gson.fromJson(item.payloadJson, CreateTransactionRequest::class.java)
                    val res = api.createTransaction(req)
                    if (res.isSuccessful) {
                        transactionDao.markAsSynced(item.entityId)
                        syncQueueDao.deleteById(item.queueId)
                        syncedCount++
                    }
                } else if (item.operation == "DELETE") {
                    api.deleteTransaction(item.entityId)
                    syncQueueDao.deleteById(item.queueId)
                    syncedCount++
                }
            } catch (e: Exception) {
                Log.w("ExpenseRepo", "Failed to sync queue item ${item.queueId}: ${e.message}")
            }
        }
        syncedCount
    }

    private suspend fun queuePendingSync(type: String, op: String, entityId: String, payload: String) {
        syncQueueDao.addToQueue(
            SyncQueueEntity(
                entityType = type,
                operation = op,
                entityId = entityId,
                payloadJson = payload
            )
        )
    }

    suspend fun seedSampleDataIfEmpty(userId: String) = withContext(Dispatchers.IO) {
        val count = transactionDao.getAllTransactions(userId).size
        if (count == 0) {
            val now = System.currentTimeMillis()
            val dayMs = 86400000L

            val seeds = listOf(
                TransactionEntity("seed_1", userId, 50000.0, "income", "Salary", "Monthly salary credited", now - (dayMs * 5), "Net Banking", "manual", "SYNCED"),
                TransactionEntity("seed_2", userId, 250.0, "expense", "Food", "Lunch at cafe", now, "UPI", "voice", "SYNCED", "I spent 250 rupees on lunch today"),
                TransactionEntity("seed_3", userId, 50.0, "expense", "Food", "Tea with snacks", now, "Cash", "voice", "SYNCED", "Today I spent 50 rupees on tea"),
                TransactionEntity("seed_4", userId, 100.0, "expense", "Travel", "Petrol for bike", now - dayMs, "UPI", "voice", "SYNCED", "Yesterday I paid 100 rupees for petrol"),
                TransactionEntity("seed_5", userId, 1200.0, "expense", "Shopping", "Cotton shirt purchase", now - (dayMs * 2), "Credit Card", "voice", "SYNCED", "I purchased a shirt for 1200 rupees"),
                TransactionEntity("seed_6", userId, 1450.0, "expense", "Bills", "Electricity current bill", now - (dayMs * 3), "UPI", "manual", "SYNCED"),
                TransactionEntity("seed_7", userId, 450.0, "expense", "Health", "Pharmacy medicines", now - (dayMs * 4), "Cash", "manual", "SYNCED")
            )
            transactionDao.insertAll(seeds)

            val cal = Calendar.getInstance()
            val m = cal.get(Calendar.MONTH) + 1
            val y = cal.get(Calendar.YEAR)
            val budgetSeeds = listOf(
                BudgetEntity("bgt_food_$m", userId, "Food", 5000.0, m, y, 80, "SYNCED"),
                BudgetEntity("bgt_travel_$m", userId, "Travel", 2000.0, m, y, 80, "SYNCED"),
                BudgetEntity("bgt_shopping_$m", userId, "Shopping", 3000.0, m, y, 80, "SYNCED"),
                BudgetEntity("bgt_bills_$m", userId, "Bills", 2500.0, m, y, 80, "SYNCED")
            )
            budgetDao.insertAll(budgetSeeds)
        }
    }
}

