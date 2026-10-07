package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EDRColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = BrandGreenSubtle,
    onPrimaryContainer = BrandGreenDark,
    secondary = BrandGreenLight,
    onSecondary = Color.White,
    secondaryContainer = CardDark2,
    onSecondaryContainer = TextPrimary,
    tertiary = EDRGreen,
    onTertiary = Color.White,
    background = BgDark,
    onBackground = TextPrimary,
    surface = CardDark,
    onSurface = TextPrimary,
    surfaceVariant = CardDark2,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    outlineVariant = BorderDark2,
    error = EDRRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EDRColorScheme,
        typography = Typography,
        content = content
    )
}
