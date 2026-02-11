package com.coinmetric.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = CoinMetricTokens.Primary,
    onPrimary = CoinMetricTokens.OnPrimary,
    secondary = CoinMetricTokens.Secondary,
    onSecondary = CoinMetricTokens.OnSecondary,
    surfaceVariant = CoinMetricTokens.SurfaceVariant,
    outline = CoinMetricTokens.Outline,
)

private val DarkColors = darkColorScheme(
    primary = CoinMetricTokens.PrimaryDark,
    onPrimary = CoinMetricTokens.OnPrimary,
    secondary = CoinMetricTokens.SecondaryDark,
    onSecondary = CoinMetricTokens.OnSecondary,
    surfaceVariant = CoinMetricTokens.SurfaceVariantDark,
    outline = CoinMetricTokens.OutlineDark,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
fun CoinMetricTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        typography = Typography(),
        shapes = AppShapes,
        content = content,
    )
}
