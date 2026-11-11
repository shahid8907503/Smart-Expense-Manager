package com.smart.expense.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    private const val CHANNEL_BUDGET_ALERTS = "budget_alerts_channel"
    private const val CHANNEL_REMINDERS = "reminders_channel"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET_ALERTS,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when spending reaches 80% or exceeds monthly budget"
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Expense Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to log daily expenses"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(budgetChannel)
            manager.createNotificationChannel(reminderChannel)
        }
    }

    fun showBudgetWarning(context: Context, category: String, spent: Double, limit: Double, isExceeded: Boolean) {
        val title = if (isExceeded) "⚠️ Budget Exceeded!" else "⚡ Budget Warning (80% Reached)"
        val message = if (isExceeded) {
            "You have spent ₹${spent.toInt()} on $category, exceeding your monthly limit of ₹${limit.toInt()}."
        } else {
            "You have used over 80% of your $category budget (₹${spent.toInt()} of ₹${limit.toInt()})."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(category.hashCode(), notification)
        } catch (e: SecurityException) {
            // Handled when notification permission is pending
        }
    }
}

