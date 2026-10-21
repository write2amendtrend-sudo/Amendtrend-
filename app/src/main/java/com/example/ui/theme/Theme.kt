package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AmendTrendDarkColorScheme = darkColorScheme(
    primary = MintGreen,
    onPrimary = DeepBackground,
    primaryContainer = DarkBlueGray,
    onPrimaryContainer = MintGreen,
    secondary = MintGreen,
    onSecondary = DeepBackground,
    secondaryContainer = CardBackground,
    onSecondaryContainer = PrimaryText,
    tertiary = AccentCyan,
    onTertiary = DeepBackground,
    background = DeepBackground,
    onBackground = PrimaryText,
    surface = CardBackground,
    onSurface = PrimaryText,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = SecondaryText,
    outline = BorderStroke,
    outlineVariant = DarkBlueGray,
    error = BearishRed,
    onError = Color.White
)

val AmendTrendLightColorScheme = AmendTrendDarkColorScheme // Dark-mode-first as requested

@Composable
fun AmendTrendTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AmendTrendDarkColorScheme,
        typography = Typography,
        content = content
    )
}
