package com.smart.expense.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.smart.expense.data.currency.CurrencyManager
import com.smart.expense.data.local.entities.TransactionEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object DataExporter {

    fun exportTransactionsToCsv(
        context: Context,
        transactions: List<TransactionEntity>,
        currencyCode: String = "INR",
        dateFormatPattern: String = "dd/MM/yyyy"
    ) {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "No transactions available to export", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(exportDir, "SmartExpense_Export_$timestamp.csv")

            val dateFormatter = SimpleDateFormat(dateFormatPattern, Locale.getDefault())

            FileWriter(file).use { writer ->
                // Write CSV Header
                writer.append("ID,Date,Description,Type,Category,Amount,Currency,Payment_Method,Payment_Type,Source,Sync_Status\n")

                for (tx in transactions) {
                    val formattedDate = dateFormatter.format(Date(tx.transactionDate))
                    val safeDescription = escapeCsv(tx.description)
                    val safeCategory = escapeCsv(tx.category)

                    writer.append("${tx.id},")
                    writer.append("$formattedDate,")
                    writer.append("$safeDescription,")
                    writer.append("${tx.type},")
                    writer.append("$safeCategory,")
                    writer.append("${tx.amount},")
                    writer.append("$currencyCode,")
                    writer.append("${tx.paymentMethod},")
                    writer.append("${tx.paymentType},")
                    writer.append("${tx.source},")
                    writer.append("${tx.syncStatus}\n")
                }
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Smart Expense Report - $timestamp")
                putExtra(Intent.EXTRA_TEXT, "Exported ${transactions.size} transactions from Smart Expense Manager.")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share / Save Expense Report CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
