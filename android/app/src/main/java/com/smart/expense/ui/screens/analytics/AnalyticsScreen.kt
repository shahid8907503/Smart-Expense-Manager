package com.smart.expense.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smart.expense.data.repository.AuthRepository
import com.smart.expense.data.repository.ExpenseRepository
import com.smart.expense.ui.components.*
import com.smart.expense.ui.theme.ForestGreenContainer
import com.smart.expense.ui.theme.ForestGreenPrimary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalyticsScreen(
    authRepository: AuthRepository,
    expenseRepository: ExpenseRepository,
    onNavigateBack: () -> Unit
) {
    val user = authRepository.currentUser.collectAsState().value
    val allTransactions by expenseRepository.getAllTransactionsFlow(user.userId).collectAsState(initial = emptyList())

    val now = Calendar.getInstance()

    // Category Breakdown Calculation
    val categorySpendItems = remember(allTransactions) {
        val expenseMap = mutableMapOf<String, Double>()
        allTransactions.filter { it.type == "expense" }.forEach { tx ->
            expenseMap[tx.category] = (expenseMap[tx.category] ?: 0.0) + tx.amount
        }
        expenseMap.map { (cat, amt) ->
            CategorySpendItem(
                category = cat,
                amount = amt,
                color = getCategoryColor(cat)
            )
        }.sortedByDescending { it.amount }
    }

    // Monthly Trends Calculation (Last 6 Months)
    val monthlyTrends = remember(allTransactions) {
        val list = mutableListOf<MonthlyTrendItem>()
        for (i in 4 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            val m = c.get(Calendar.MONTH)
            val y = c.get(Calendar.YEAR)
            val mName = SimpleDateFormat("MMM", Locale.US).format(c.time)

            val inc = allTransactions.filter {
                val d = Calendar.getInstance().apply { timeInMillis = it.transactionDate }
                it.type == "income" && d.get(Calendar.MONTH) == m && d.get(Calendar.YEAR) == y
            }.sumOf { it.amount }

            val exp = allTransactions.filter {
                val d = Calendar.getInstance().apply { timeInMillis = it.transactionDate }
                it.type == "expense" && d.get(Calendar.MONTH) == m && d.get(Calendar.YEAR) == y
            }.sumOf { it.amount }

            list.add(MonthlyTrendItem(mName, inc, exp))
        }
        list
    }

    // Summary calculations
    val calToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }
    val todaySpending = allTransactions.filter { it.type == "expense" && it.transactionDate >= calToday.timeInMillis }.sumOf { it.amount }

    val calWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    val weekSpending = allTransactions.filter { it.type == "expense" && it.transactionDate >= calWeek.timeInMillis }.sumOf { it.amount }

    val calMonth = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    val monthSpending = allTransactions.filter { it.type == "expense" && it.transactionDate >= calMonth.timeInMillis }.sumOf { it.amount }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Financial Insights",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Spending Summaries Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryPill("Today", todaySpending, Modifier.weight(1f))
                SummaryPill("Last 7 Days", weekSpending, Modifier.weight(1f))
                SummaryPill("This Month", monthSpending, Modifier.weight(1f))
            }

            // Category Breakdown Chart Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Category Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CategorySpendingChart(items = categorySpendItems)
                }
            }

            // Monthly Trend Chart Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Income vs Expense Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    MonthlyTrendChart(trends = monthlyTrends)
                }
            }

            // Spending Insight Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ForestGreenContainer)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Smart Insight: Your highest spending category is ${categorySpendItems.firstOrNull()?.category ?: "Food"}. Keeping track with voice commands prevents untracked expenses!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ForestGreenPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SummaryPill(label: String, amount: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "₹${String.format(Locale.US, "%,.0f", amount)}",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

