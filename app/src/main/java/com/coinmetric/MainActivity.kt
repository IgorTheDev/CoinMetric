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
        
        // Initialize GoogleAuthManager before setContent to avoid lifecycle issues
        googleAuthManager = GoogleAuthManager(this, null) // We'll pass the VM later
        
        setContent {
            // Initialize the ViewModel inside the composable context
            val vm: CoinMetricViewModel = viewModel()
            
            // Update the auth manager with the VM reference
            googleAuthManager.setViewModel(vm)
            
            DisposableEffect(googleAuthManager) {
                lifecycle.addObserver(googleAuthManager)
                onDispose {
                    lifecycle.removeObserver(googleAuthManager)
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
