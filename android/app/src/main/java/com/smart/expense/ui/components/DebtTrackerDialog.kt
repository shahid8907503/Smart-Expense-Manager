package com.smart.expense.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smart.expense.data.currency.CurrencyManager
import com.smart.expense.ui.theme.EmeraldAccent
import com.smart.expense.ui.theme.ExpenseMutedRed
import com.smart.expense.ui.theme.ForestGreenContainer
import com.smart.expense.ui.theme.ForestGreenPrimary
import java.text.SimpleDateFormat
import java.util.*

data class DebtRecord(
    val id: String = UUID.randomUUID().toString(),
    val personName: String,
    val amount: Double,
    val isOwedToMe: Boolean, // true = Lent (They owe me), false = Borrowed (I owe them)
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSettled: Boolean = false
)

/**
 * Debt / Borrowing & Lending Manager Dialog
 * Direct implementation of the "Debt" module from Bishinews Expense Manager
 */
@Composable
fun DebtTrackerDialog(
    currencyCode: String = "INR",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("smart_expense_debts", Context.MODE_PRIVATE) }
    val gson = remember { Gson() }

    fun loadDebts(): List<DebtRecord> {
        val json = prefs.getString("debts_list", null) ?: return emptyList()
        val type = object : TypeToken<List<DebtRecord>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveDebts(list: List<DebtRecord>) {
        prefs.edit().putString("debts_list", gson.toJson(list)).apply()
    }

    var debts by remember { mutableStateOf(loadDebts()) }
    var isAddingNew by remember { mutableStateOf(false) }

    var personNameInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var isOwedToMeInput by remember { mutableStateOf(true) }
    var noteInput by remember { mutableStateOf("") }

    val activeDebts = debts.filter { !it.isSettled }
    val totalOwedToMe = activeDebts.filter { it.isOwedToMe }.sumOf { it.amount }
    val totalIOwe = activeDebts.filter { !it.isOwedToMe }.sumOf { it.amount }

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
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
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text = "Debt & IOU Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary Row: You are owed vs You owe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EmeraldAccent.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("They Owe Me", style = MaterialTheme.typography.labelSmall, color = EmeraldAccent)
                            Text(
                                text = CurrencyManager.format(totalOwedToMe, currencyCode),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = ExpenseMutedRed.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("I Owe Them", style = MaterialTheme.typography.labelSmall, color = ExpenseMutedRed)
                            Text(
                                text = CurrencyManager.format(totalIOwe, currencyCode),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseMutedRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isAddingNew) {
                    // Add New Form
                    Text("Add Person & Amount", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = personNameInput,
                        onValueChange = { personNameInput = it },
                        label = { Text("Person Name (e.g., Rahul)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount (${CurrencyManager.getCurrency(currencyCode).symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Type Selector: Lent vs Borrowed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isOwedToMeInput,
                            onClick = { isOwedToMeInput = true },
                            label = { Text("I Lent Money", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isOwedToMeInput,
                            onClick = { isOwedToMeInput = false },
                            label = { Text("I Borrowed Money", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isAddingNew = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val amt = amountInput.toDoubleOrNull() ?: 0.0
                                if (personNameInput.isNotBlank() && amt > 0) {
                                    val newRecord = DebtRecord(
                                        personName = personNameInput.trim(),
                                        amount = amt,
                                        isOwedToMe = isOwedToMeInput,
                                        note = noteInput.trim()
                                    )
                                    val updated = debts + newRecord
                                    debts = updated
                                    saveDebts(updated)
                                    personNameInput = ""
                                    amountInput = ""
                                    isAddingNew = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                    }
                } else {
                    // List of Debts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Records (${activeDebts.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { isAddingNew = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Record", fontSize = 12.sp)
                        }
                    }

                    if (activeDebts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active debts recorded.\nTap '+ Add Record' to track money lent or borrowed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(activeDebts) { record ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(record.personName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = if (record.isOwedToMe) "Owes you • Lent" else "You owe • Borrowed",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (record.isOwedToMe) EmeraldAccent else ExpenseMutedRed
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = CurrencyManager.format(record.amount, currencyCode),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (record.isOwedToMe) EmeraldAccent else ExpenseMutedRed
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = {
                                                    val updated = debts.map {
                                                        if (it.id == record.id) it.copy(isSettled = true) else it
                                                    }
                                                    debts = updated
                                                    saveDebts(updated)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Settle",
                                                    tint = ForestGreenPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

