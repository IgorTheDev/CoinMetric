package com.coinmetric

import android.app.Application
import com.google.firebase.FirebaseApp

class CoinMetricApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
