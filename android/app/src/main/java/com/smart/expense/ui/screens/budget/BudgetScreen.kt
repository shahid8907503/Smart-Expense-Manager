package com.smart.expense.ui.screens.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smart.expense.data.repository.AuthRepository
import com.smart.expense.data.repository.ExpenseRepository
import com.smart.expense.ui.components.BudgetProgressItem
import com.smart.expense.ui.theme.ForestGreenPrimary
import com.smart.expense.voice.KeywordCategorizer
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    authRepository: AuthRepository,
    expenseRepository: ExpenseRepository,
    onNavigateBack: () -> Unit
) {
    val user = authRepository.currentUser.collectAsState().value
    val scope = rememberCoroutineScope()

    val cal = Calendar.getInstance()
    val currentMonth = cal.get(Calendar.MONTH) + 1
    val currentYear = cal.get(Calendar.YEAR)

    val budgets by expenseRepository.getBudgetsFlow(user.userId, currentMonth, currentYear).collectAsState(initial = emptyList())
    val transactions by expenseRepository.getAllTransactionsFlow(user.userId).collectAsState(initial = emptyList())

    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Food") }
    var selectedPeriod by remember { mutableStateOf("MONTHLY") } // WEEKLY, MONTHLY, ANNUAL
    var limitInput by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Budget Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = { showSetBudgetDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Set Budget", tint = ForestGreenPrimary)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Track your spending limits across Weekly, Monthly, and Annual periods with proactive alerts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Weekly, Monthly, Annual Period Tabs
                val periods = listOf("WEEKLY" to "Weekly", "MONTHLY" to "Monthly", "ANNUAL" to "Annual")
                TabRow(
                    selectedTabIndex = periods.indexOfFirst { it.first == selectedPeriod },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = ForestGreenPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    periods.forEachIndexed { index, (key, label) ->
                        Tab(
                            selected = selectedPeriod == key,
                            onClick = { selectedPeriod = key },
                            text = {
                                Text(
                                    text = label,
                                    fontWeight = if (selectedPeriod == key) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedPeriod == key) ForestGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }

            if (budgets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No budgets created yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showSetBudgetDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                            ) {
                                Text("Create Budget")
                            }
                        }
                    }
                }
            } else {
                items(budgets) { budget ->
                    val now = System.currentTimeMillis()
                    val periodTransactions = when (selectedPeriod) {
                        "WEEKLY" -> transactions.filter { it.transactionDate >= now - (7L * 24 * 60 * 60 * 1000) }
                        "ANNUAL" -> {
                            val calYear = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0) }
                            transactions.filter { it.transactionDate >= calYear.timeInMillis }
                        }
                        else -> {
                            val calMonth = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0) }
                            transactions.filter { it.transactionDate >= calMonth.timeInMillis }
                        }
                    }

                    val spent = periodTransactions
                        .filter { it.type == "expense" && it.category.equals(budget.category, ignoreCase = true) }
                        .sumOf { it.amount }

                    val effectiveLimit = when (selectedPeriod) {
                        "WEEKLY" -> budget.monthlyLimit / 4.0
                        "ANNUAL" -> budget.monthlyLimit * 12.0
                        else -> budget.monthlyLimit
                    }

                    BudgetProgressItem(
                        category = budget.category,
                        monthlyLimit = effectiveLimit,
                        spent = spent
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Set Budget Dialog
    if (showSetBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showSetBudgetDialog = false },
            title = { Text("Set Monthly Budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            KeywordCategorizer.CATEGORIES.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it },
                        label = { Text("Monthly Limit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = limitInput.toDoubleOrNull()
                        if (limit != null && limit > 0) {
                            scope.launch {
                                expenseRepository.setBudget(
                                    userId = user.userId,
                                    category = selectedCategory,
                                    monthlyLimit = limit,
                                    month = currentMonth,
                                    year = currentYear
                                )
                            }
                            showSetBudgetDialog = false
                            limitInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Text("Save Budget")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

