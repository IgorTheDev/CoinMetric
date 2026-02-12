package com.coinmetric.ui

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class RecurringPaymentReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        LimitNotificationHelper(applicationContext).notifyRecurringPaymentReminder(
            paymentName = "Постоянные платежи",
        )
        return Result.success()
    }
}
