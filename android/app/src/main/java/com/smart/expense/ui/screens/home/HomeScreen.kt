package com.smart.expense.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.smart.expense.data.currency.CurrencyManager
import com.smart.expense.data.local.entities.TransactionEntity
import com.smart.expense.data.repository.AuthRepository
import com.smart.expense.data.repository.ExpenseRepository
import com.smart.expense.ui.components.BudgetProgressItem
import com.smart.expense.ui.components.DebtTrackerDialog
import com.smart.expense.ui.components.FinancialHealthCard
import com.smart.expense.ui.components.FinancialSummaryCard
import com.smart.expense.ui.components.MultiTimelineBreakdownCard
import com.smart.expense.ui.components.TransactionRowItem
import com.smart.expense.util.DataExporter
import com.smart.expense.ui.screens.voice.VoiceConfirmationDialog
import com.smart.expense.ui.screens.voice.VoiceListeningDialog
import com.smart.expense.ui.theme.*
import com.smart.expense.voice.ParsedVoiceTransaction
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    authRepository: AuthRepository,
    expenseRepository: ExpenseRepository,
    onNavigateToActivity: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToVoiceQuery: () -> Unit,
    onNavigateToAddManual: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val user = authRepository.currentUser.collectAsState().value
    val scope = rememberCoroutineScope()

    val allTransactions by expenseRepository.getAllTransactionsFlow(user.userId).collectAsState(initial = emptyList())
    val recentTransactions by expenseRepository.getRecentTransactionsFlow(user.userId, limit = 5).collectAsState(initial = emptyList())

    val cal = Calendar.getInstance()
    val currentMonth = cal.get(Calendar.MONTH) + 1
    val currentYear = cal.get(Calendar.YEAR)
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())

    val budgets by expenseRepository.getBudgetsFlow(user.userId, currentMonth, currentYear).collectAsState(initial = emptyList())

    // Calculations
    val totalIncome = allTransactions.filter { it.type == "income" }.sumOf { it.amount }
    val totalExpense = allTransactions.filter { it.type == "expense" }.sumOf { it.amount }
    val availableBalance = totalIncome - totalExpense

    // Voice Dialogs State
    val context = LocalContext.current
    var showListeningDialog by remember { mutableStateOf(false) }
    var parsedVoiceTx by remember { mutableStateOf<ParsedVoiceTransaction?>(null) }
    var showDebtTracker by remember { mutableStateOf(false) }

    if (showDebtTracker) {
        DebtTrackerDialog(
            currencyCode = user.preferredCurrency,
            onDismiss = { showDebtTracker = false }
        )
    }

    Scaffold(
        floatingActionButton = {
            // Quick manual add transaction FAB
            FloatingActionButton(
                onClick = onNavigateToAddManual,
                containerColor = ForestGreenPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Manually")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))

                // Personalized Greeting & Month Selector Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Good day, ${user.name.split(" ").firstOrNull() ?: "there"}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = monthName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Avatar button navigating to Profile
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ForestGreenContainer)
                            .clickable { onNavigateToProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user.name.firstOrNull() ?: 'U').toString().uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = ForestGreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 1. Available Balance Hero Card (Dynamic Multi-Currency)
            item {
                val curr = CurrencyManager.getCurrency(user.preferredCurrency)
                FinancialSummaryCard(
                    balance = availableBalance * curr.exchangeRateFromInr,
                    totalIncome = totalIncome * curr.exchangeRateFromInr,
                    totalExpense = totalExpense * curr.exchangeRateFromInr,
                    currencySymbol = curr.symbol
                )
            }

            // 1a. Multi-Timeline Financial Breakdown Table & Quick Grid (from Bishinews Personal Expense)
            item {
                MultiTimelineBreakdownCard(
                    allTransactions = allTransactions,
                    currencyCode = user.preferredCurrency,
                    onNavigateToBudgets = onNavigateToBudgets,
                    onNavigateToActivity = onNavigateToActivity,
                    onOpenDebtTracker = { showDebtTracker = true },
                    onExportCsv = {
                        DataExporter.exportTransactionsToCsv(
                            context = context,
                            transactions = allTransactions,
                            currencyCode = user.preferredCurrency,
                            dateFormatPattern = user.dateFormat
                        )
                    }
                )
            }

            // 1b. AI Financial Health & Spending Velocity Card
            item {
                FinancialHealthCard(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    transactions = allTransactions
                )
            }

            // 2. The Main Distinctive Voice-Entry Interaction
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showListeningDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldAccent)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "INTELLIGENT VOICE ENTRY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldAccent,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add Expense by Voice",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Say \"I spent ₹250 on lunch today\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        // Distinctive Mic Circle
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(EmeraldAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice microphone",
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Quick Voice Query Assistant Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .clickable { onNavigateToVoiceQuery() }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ForestGreenContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hearing,
                                    contentDescription = null,
                                    tint = ForestGreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Ask Financial Questions",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "\"How much did I spend on food this month?\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Monthly Budget Progress (Top Categories)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly Budgets",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToBudgets) {
                        Text(
                            text = "View all",
                            color = ForestGreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (budgets.isNotEmpty()) {
                val topBudget = budgets.first()
                val spent = allTransactions
                    .filter { it.type == "expense" && it.category.equals(topBudget.category, ignoreCase = true) }
                    .sumOf { it.amount }

                item {
                    BudgetProgressItem(
                        category = topBudget.category,
                        monthlyLimit = topBudget.monthlyLimit,
                        spent = spent
                    )
                }
            }

            // 4. Recent Transactions Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToActivity) {
                        Text(
                            text = "See all",
                            color = ForestGreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent transactions. Tap microphone to add one!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(recentTransactions) { tx ->
                    TransactionRowItem(
                        transaction = tx,
                        currencyCode = user.preferredCurrency,
                        onClick = onNavigateToActivity
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Voice Listening Dialog Modal
    if (showListeningDialog) {
        VoiceListeningDialog(
            preferredLanguage = user.preferredLanguage,
            onDismiss = { showListeningDialog = false },
            onTransactionParsed = { parsed ->
                showListeningDialog = false
                parsedVoiceTx = parsed
            }
        )
    }

    // Voice Confirmation & Edit Dialog Modal
    parsedVoiceTx?.let { parsed ->
        VoiceConfirmationDialog(
            initialData = parsed,
            onDismiss = { parsedVoiceTx = null },
            onConfirm = { amount, type, category, desc, timestamp, paymentMethod, paymentType ->
                scope.launch {
                    expenseRepository.addTransaction(
                        userId = user.userId,
                        amount = amount,
                        type = type,
                        category = category,
                        description = desc,
                        timestamp = timestamp,
                        paymentMethod = paymentMethod,
                        paymentType = paymentType,
                        source = "voice",
                        rawVoiceText = parsed.rawText
                    )
                }
                parsedVoiceTx = null
            }
        )
    }
}

