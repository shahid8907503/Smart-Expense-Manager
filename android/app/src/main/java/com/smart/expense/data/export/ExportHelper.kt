package com.smart.expense.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.smart.expense.data.local.entities.TransactionEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object ExportHelper {

    /**
     * Exports transaction list to a standard CSV format and shares it via Intent.
     */
    fun exportToCsv(context: Context, transactions: List<TransactionEntity>): File? {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(exportDir, "SmartExpense_Export_$timestamp.csv")

            val writer = FileWriter(file)
            // CSV Header
            writer.append("ID,Date,Type,Category,Description,Amount (INR),Payment Method,Source,Sync Status\n")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            for (tx in transactions) {
                val dateStr = dateFormat.format(Date(tx.transactionDate))
                val cleanDesc = tx.description.replace(",", " ")
                writer.append("${tx.id},${dateStr},${tx.type},${tx.category},${cleanDesc},${tx.amount},${tx.paymentMethod},${tx.source},${tx.syncStatus}\n")
            }

            writer.flush()
            writer.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates an Android share sheet intent for the exported CSV file.
     */
    fun shareCsvFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Financial Transaction Report - Smart Expense")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Transaction Report"))
    }
}

