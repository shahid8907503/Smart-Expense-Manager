package com.smart.expense.ui.screens.manual

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smart.expense.data.repository.AuthRepository
import com.smart.expense.data.repository.ExpenseRepository
import com.smart.expense.ui.theme.ExpenseMutedRed
import com.smart.expense.ui.theme.ForestGreenPrimary
import com.smart.expense.ui.theme.PositiveIncomeGreen
import com.smart.expense.voice.KeywordCategorizer
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    authRepository: AuthRepository,
    expenseRepository: ExpenseRepository,
    onNavigateBack: () -> Unit
) {
    val user = authRepository.currentUser.collectAsState().value
    val scope = rememberCoroutineScope()

    var amountText by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("expense") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var description by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("UPI") }
    var relativeDay by remember { mutableStateOf("Today") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val paymentMethods = listOf("UPI", "Cash", "Credit Card", "Debit Card", "Net Banking")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Manual Entry",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Type Toggle: Expense / Income
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isExpense = transactionType == "expense"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isExpense) ExpenseMutedRed else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        transactionType = "expense"
                        if (selectedCategory == "Salary") selectedCategory = "Food"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Expense",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (!isExpense) PositiveIncomeGreen else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        transactionType = "income"
                        selectedCategory = "Salary"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Income",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = if (!isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Amount Input
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it; errorMessage = null },
            label = { Text("Amount") },
            leadingIcon = {
                Text(
                    text = "₹",
                    style = MaterialTheme.typography.titleLarge,
                    color = ForestGreenPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description Input (with auto-categorization hint)
        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                errorMessage = null
                if (transactionType == "expense") {
                    val autoCat = KeywordCategorizer.categorize(it)
                    if (autoCat != "Other") selectedCategory = autoCat
                }
            },
            label = { Text("Description (e.g. Lunch, Petrol, Shirt)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Category Selection Chips
        Text(
            text = "Category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = if (transactionType == "income") {
                listOf("Salary", "Investment", "Freelance", "Gift", "Other")
            } else {
                KeywordCategorizer.CATEGORIES
            }

            categories.forEach { cat ->
                val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreenPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Date selector
        Text(
            text = "Date",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("Today", "Yesterday", "Day Before").forEach { d ->
                FilterChip(
                    selected = relativeDay == d,
                    onClick = { relativeDay = d },
                    label = { Text(d) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreenPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Payment Method Selector
        Text(
            text = "Payment Method",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            paymentMethods.forEach { method ->
                val isSelected = selectedPaymentMethod == method
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedPaymentMethod = method },
                    label = { Text(method) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreenPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Save Button
        Button(
            onClick = {
                val amt = amountText.toDoubleOrNull()
                if (amt == null || amt <= 0.0) {
                    errorMessage = "Please enter a valid amount"
                    return@Button
                }
                if (description.isBlank()) {
                    errorMessage = "Please enter a description"
                    return@Button
                }

                val cal = Calendar.getInstance()
                when (relativeDay) {
                    "Yesterday" -> cal.add(Calendar.DAY_OF_YEAR, -1)
                    "Day Before" -> cal.add(Calendar.DAY_OF_YEAR, -2)
                }

                isSaving = true
                scope.launch {
                    expenseRepository.addTransaction(
                        userId = user.userId,
                        amount = amt,
                        type = transactionType,
                        category = selectedCategory,
                        description = description.trim(),
                        timestamp = cal.timeInMillis,
                        paymentMethod = selectedPaymentMethod,
                        source = "manual"
                    )
                    isSaving = false
                    onNavigateBack()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Save Transaction", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

