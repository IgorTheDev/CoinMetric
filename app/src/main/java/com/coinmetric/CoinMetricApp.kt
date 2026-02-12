package com.coinmetric

import android.app.Application
import com.coinmetric.sync.FirestoreSyncService
import com.coinmetric.ui.SyncNotificationHelper
import com.google.firebase.FirebaseApp

class CoinMetricApp : Application() {
    companion object {
        lateinit var instance: CoinMetricApp
            private set
        lateinit var syncService: FirestoreSyncService
            private set
        lateinit var syncNotificationHelper: SyncNotificationHelper
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        FirebaseApp.initializeApp(this)
        syncService = FirestoreSyncService()
        syncNotificationHelper = SyncNotificationHelper(this)
    }
}
