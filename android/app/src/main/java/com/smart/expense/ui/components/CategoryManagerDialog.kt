package com.smart.expense.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.smart.expense.ui.theme.ForestGreenContainer
import com.smart.expense.ui.theme.ForestGreenPrimary

@Composable
fun CategoryManagerDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("smart_expense_categories", Context.MODE_PRIVATE) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Expense, 1: Income, 2: Payees, 3: Tags
    var newEntryName by remember { mutableStateOf("") }

    val defaultExpenseCategories = remember {
        listOf("Food & Dining", "Groceries", "Shopping", "Transport", "Bills & Utilities", "Health", "Entertainment", "Rent", "Education", "Travel")
    }
    val defaultIncomeCategories = remember {
        listOf("Salary", "Freelance", "Investment Returns", "Scholarship", "Rental Income", "Gifts", "Cashback", "Other")
    }
    val defaultPayees = remember {
        listOf("Zomato", "Swiggy", "Amazon", "Flipkart", "Uber", "Metro", "Landlord", "College Canteen", "Electricity Board")
    }
    val defaultTags = remember {
        listOf("#College", "#Hostel", "#Weekend", "#SemesterExam", "#Trip", "#Family", "#Office")
    }

    var customExpenses by remember {
        mutableStateOf(prefs.getStringSet("custom_expense_cats", emptySet())?.toList() ?: emptyList())
    }
    var customIncome by remember {
        mutableStateOf(prefs.getStringSet("custom_income_cats", emptySet())?.toList() ?: emptyList())
    }
    var customPayees by remember {
        mutableStateOf(prefs.getStringSet("custom_payees", emptySet())?.toList() ?: emptyList())
    }
    var customTags by remember {
        mutableStateOf(prefs.getStringSet("custom_tags", emptySet())?.toList() ?: emptyList())
    }

    val currentList = when (selectedTab) {
        0 -> defaultExpenseCategories + customExpenses
        1 -> defaultIncomeCategories + customIncome
        2 -> defaultPayees + customPayees
        else -> defaultTags + customTags
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ForestGreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(20.dp))
                        }
                        Text("Category & Payee Manager", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Switcher
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = ForestGreenPrimary
                ) {
                    listOf("Expense", "Income", "Payee", "Tags").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Add Custom Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newEntryName,
                        onValueChange = { newEntryName = it },
                        label = { Text("Add New ${listOf("Expense Category", "Income Category", "Payee", "Tag")[selectedTab]}") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            val trimmed = newEntryName.trim()
                            if (trimmed.isNotBlank()) {
                                when (selectedTab) {
                                    0 -> {
                                        val updated = customExpenses + trimmed
                                        customExpenses = updated
                                        prefs.edit().putStringSet("custom_expense_cats", updated.toSet()).apply()
                                    }
                                    1 -> {
                                        val updated = customIncome + trimmed
                                        customIncome = updated
                                        prefs.edit().putStringSet("custom_income_cats", updated.toSet()).apply()
                                    }
                                    2 -> {
                                        val updated = customPayees + trimmed
                                        customPayees = updated
                                        prefs.edit().putStringSet("custom_payees", updated.toSet()).apply()
                                    }
                                    3 -> {
                                        val tagFormatted = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
                                        val updated = customTags + tagFormatted
                                        customTags = updated
                                        prefs.edit().putStringSet("custom_tags", updated.toSet()).apply()
                                    }
                                }
                                Toast.makeText(context, "Added: $trimmed", Toast.LENGTH_SHORT).show()
                                newEntryName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Active Items (${currentList.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Flow / Wrapping List of Categories
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentList) { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Ready",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ForestGreenPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

