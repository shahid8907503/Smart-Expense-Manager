package com.smart.expense

import android.app.Application
import com.smart.expense.data.notifications.NotificationHelper
import com.smart.expense.data.sync.SyncManager

class SmartExpenseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize notification channels for budget alerts
        NotificationHelper.initNotificationChannels(this)
        // Schedule background synchronization with WorkManager
        SyncManager.schedulePeriodicSync(this)
    }
}

