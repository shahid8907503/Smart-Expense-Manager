package com.smart.expense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smart.expense.data.currency.CurrencyManager
import com.smart.expense.data.local.entities.TransactionEntity
import com.smart.expense.ui.theme.EmeraldAccent
import com.smart.expense.ui.theme.ExpenseMutedRed
import com.smart.expense.ui.theme.ForestGreenContainer
import com.smart.expense.ui.theme.ForestGreenPrimary
import java.text.SimpleDateFormat
import java.util.*

data class TimelinePeriodData(
    val title: String,
    val dateRangeLabel: String,
    val income: Double,
    val expense: Double,
    val balance: Double
)

/**
 * Multi-Timeline Financial Breakdown Card & Quick Action Matrix
 * Inspired by Personal Expense Dashboard (com.expensemanager by Bishinews)
 */
@Composable
fun MultiTimelineBreakdownCard(
    allTransactions: List<TransactionEntity>,
    currencyCode: String = "INR",
    onNavigateToBudgets: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onOpenDebtTracker: () -> Unit = {},
    onExportCsv: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cal = Calendar.getInstance()
    val nowMs = System.currentTimeMillis()

    // 1. Today's start
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStartMs = cal.timeInMillis

    // 2. This Week's start (Monday)
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val weekStartMs = cal.timeInMillis

    // 3. This Month's start
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val monthStartMs = cal.timeInMillis

    // 4. Year to Date start (Jan 1)
    cal.set(Calendar.DAY_OF_YEAR, 1)
    val yearStartMs = cal.timeInMillis

    fun computePeriod(startMs: Long): Pair<Double, Double> {
        val filtered = allTransactions.filter { it.transactionDate in startMs..nowMs }
        val income = filtered.filter { it.type == "income" }.sumOf { it.amount }
        val expense = filtered.filter { it.type == "expense" }.sumOf { it.amount }
        return Pair(income, expense)
    }

    val todayPair = computePeriod(todayStartMs)
    val weekPair = computePeriod(weekStartMs)
    val monthPair = computePeriod(monthStartMs)
    val yearPair = computePeriod(yearStartMs)

    val dateFormat = SimpleDateFormat("MM-dd", Locale.US)
    val todayDateStr = SimpleDateFormat("yyyy-MM-dd EEE", Locale.US).format(Date(nowMs))
    val weekRangeStr = "${dateFormat.format(Date(weekStartMs))} - ${dateFormat.format(Date(nowMs))}"
    val monthRangeStr = "${dateFormat.format(Date(monthStartMs))} - ${dateFormat.format(Date(nowMs))}"
    val yearRangeStr = "${SimpleDateFormat("yyyy-01-01", Locale.US).format(Date(nowMs))} - Today"

    val periods = listOf(
        TimelinePeriodData("Today", todayDateStr, todayPair.first, todayPair.second, todayPair.first - todayPair.second),
        TimelinePeriodData("This Week", weekRangeStr, weekPair.first, weekPair.second, weekPair.first - weekPair.second),
        TimelinePeriodData("This Month", monthRangeStr, monthPair.first, monthPair.second, monthPair.first - monthPair.second),
        TimelinePeriodData("Year to Date", yearRangeStr, yearPair.first, yearPair.second, yearPair.first - yearPair.second)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Top Quick Action Grid (Bishinews Quick Buttons: Budget, Debt, Transfer, Export)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QuickActionButton(
                    icon = Icons.Default.PieChart,
                    label = "Budgets",
                    onClick = onNavigateToBudgets
                )
                QuickActionButton(
                    icon = Icons.Default.ReceiptLong,
                    label = "Activity",
                    onClick = onNavigateToActivity
                )
                QuickActionButton(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = "Debt / IOU",
                    onClick = onOpenDebtTracker
                )
                QuickActionButton(
                    icon = Icons.Default.FileDownload,
                    label = "Export CSV",
                    onClick = onExportCsv
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Breakdown Table Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timeline Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Income / Expense / Net",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timeline Rows (Today, This Week, This Month, Year to Date)
            periods.forEachIndexed { index, p ->
                TimelineRowItem(
                    period = p,
                    currencyCode = currencyCode,
                    onClick = onNavigateToActivity
                )
                if (index < periods.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(ForestGreenContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = ForestGreenPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TimelineRowItem(
    period: TimelinePeriodData,
    currencyCode: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.3f)) {
            Text(
                text = period.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = period.dateRangeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Income & Expense
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(1.1f)
        ) {
            Text(
                text = "+${CurrencyManager.format(period.income, currencyCode)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = EmeraldAccent
            )
            Text(
                text = "-${CurrencyManager.format(period.expense, currencyCode)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ExpenseMutedRed
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Net Balance
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Net Balance",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = CurrencyManager.format(period.balance, currencyCode),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (period.balance >= 0) ForestGreenPrimary else ExpenseMutedRed
            )
        }
    }
}

