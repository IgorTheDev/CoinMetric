package com.coinmetric.ui

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object RecurringReminderScheduler {
    private const val REMINDER_WORK_NAME = "coinmetric_recurring_payment_reminder"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecurringPaymentReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(nextNineAmDelay())
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK_NAME)
    }

    private fun nextNineAmDelay(): Duration {
        val now = LocalDateTime.now()
        var nextRun = now.withHour(9).withMinute(0).withSecond(0).withNano(0)
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1)
        }
        return Duration.between(now, nextRun)
    }
}
