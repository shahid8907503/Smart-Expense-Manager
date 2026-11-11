package com.smart.expense.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smart.expense.ui.theme.ExpenseMutedRed
import com.smart.expense.ui.theme.PositiveIncomeGreen

data class MonthlyTrendItem(
    val monthName: String,
    val income: Double,
    val expense: Double
)

@Composable
fun MonthlyTrendChart(
    trends: List<MonthlyTrendItem>,
    modifier: Modifier = Modifier
) {
    if (trends.isEmpty()) return

    val maxAmount = trends.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(100.0) ?: 100.0
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(trends) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(900))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PositiveIncomeGreen))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.width(16.dp))

            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ExpenseMutedRed))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Canvas for Dual Bars
        Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartHeight = size.height - 24.dp.toPx()
                val groupCount = trends.size
                val groupWidth = size.width / groupCount
                val barWidth = 10.dp.toPx()
                val barGap = 4.dp.toPx()

                for (i in 0 until groupCount) {
                    val item = trends[i]
                    val groupCenterX = (i * groupWidth) + (groupWidth / 2f)

                    // Income bar
                    val incomeHeight = (item.income / maxAmount).toFloat() * chartHeight * animatedProgress.value
                    val incomeX = groupCenterX - barWidth - (barGap / 2f)
                    val incomeY = chartHeight - incomeHeight

                    drawRoundRect(
                        color = PositiveIncomeGreen,
                        topLeft = Offset(incomeX, incomeY),
                        size = Size(barWidth, incomeHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )

                    // Expense bar
                    val expenseHeight = (item.expense / maxAmount).toFloat() * chartHeight * animatedProgress.value
                    val expenseX = groupCenterX + (barGap / 2f)
                    val expenseY = chartHeight - expenseHeight

                    drawRoundRect(
                        color = ExpenseMutedRed,
                        topLeft = Offset(expenseX, expenseY),
                        size = Size(barWidth, expenseHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }

        // Month Names Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            trends.forEach { trend ->
                Text(
                    text = trend.monthName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

