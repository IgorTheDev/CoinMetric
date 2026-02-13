package com.coinmetric

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coinmetric.auth.GoogleAuthManager
import com.coinmetric.ui.CoinMetricRoot
import com.coinmetric.ui.CoinMetricViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var googleAuthManager: GoogleAuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            // Initialize the ViewModel inside the composable context
            val vm: CoinMetricViewModel = viewModel()
            
            // Initialize GoogleAuthManager
            val authManager = GoogleAuthManager(this, vm)
            DisposableEffect(authManager) {
                lifecycle.addObserver(authManager)
                onDispose {
                    lifecycle.removeObserver(authManager)
                }
            }
            
            val requestedRoute = intent?.getStringExtra(EXTRA_START_ROUTE)
            CoinMetricRoot(startRoute = requestedRoute)
        }
    }

    companion object {
        const val EXTRA_START_ROUTE = "coinmetric_start_route"
        const val ROUTE_HOME = "home"
        const val ROUTE_ANALYTICS = "analytics"
    }
}
