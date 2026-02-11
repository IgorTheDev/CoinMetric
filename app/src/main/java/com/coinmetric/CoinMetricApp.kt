package com.coinmetric

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.coinmetric.sync.GoogleSyncWorker
import java.util.concurrent.TimeUnit

class CoinMetricApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val syncRequest = PeriodicWorkRequestBuilder<GoogleSyncWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "coinmetric_google_sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest,
        )
    }
}
