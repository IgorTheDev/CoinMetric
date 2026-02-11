package com.coinmetric

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.coinmetric.ui.CoinMetricRoot
import com.coinmetric.ui.theme.CoinMetricTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoinMetricTheme {
                CoinMetricRoot()
            }
        }
    }
}
