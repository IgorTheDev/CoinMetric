package com.coinmetric

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.coinmetric.ui.CoinMetricRoot
import com.coinmetric.ui.theme.CoinMetricTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var darkTheme by rememberSaveable { mutableStateOf(false) }
            CoinMetricTheme(useDarkTheme = darkTheme) {
                CoinMetricRoot(
                    darkTheme = darkTheme,
                    onDarkThemeChanged = { darkTheme = it },
                )
            }
        }
    }
}
