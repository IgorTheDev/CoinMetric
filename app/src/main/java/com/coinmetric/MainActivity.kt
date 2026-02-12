package com.coinmetric

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.coinmetric.ui.CoinMetricRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val requestedRoute = intent?.getStringExtra(EXTRA_START_ROUTE)
        setContent {
            CoinMetricRoot(startRoute = requestedRoute)
        }
    }

    companion object {
        const val EXTRA_START_ROUTE = "coinmetric_start_route"
        const val ROUTE_HOME = "home"
        const val ROUTE_ANALYTICS = "analytics"
    }
}
