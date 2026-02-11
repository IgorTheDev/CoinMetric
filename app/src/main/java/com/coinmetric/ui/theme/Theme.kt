package com.coinmetric.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0E9F6E),
    onPrimary = Color.White,
    secondary = Color(0xFF3D8B73),
    tertiary = Color(0xFF2F6A58),
    error = Color(0xFFB3261E),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF47D7A4),
    secondary = Color(0xFF79D9B8),
    tertiary = Color(0xFF95D9C5),
    error = Color(0xFFF2B8B5),
)

private val CoinMetricShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun CoinMetricTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        shapes = CoinMetricShapes,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}
