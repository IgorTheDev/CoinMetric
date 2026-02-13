package com.coinmetric

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coinmetric.auth.GoogleAuthManager
import com.coinmetric.ui.CoinMetricRoot
import com.coinmetric.ui.CoinMetricViewModel

class MainActivity : ComponentActivity() {
    private lateinit var googleAuthManager: GoogleAuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize the ViewModel
        val vm: CoinMetricViewModel = viewModel()
        
        // Initialize GoogleAuthManager
        googleAuthManager = GoogleAuthManager(this, vm)
        lifecycle.addObserver(googleAuthManager)
        
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
