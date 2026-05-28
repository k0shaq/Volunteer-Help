package com.example.volunteerhelp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GraphitePrimaryDark,
    secondary = GraphiteSecondaryDark,
    tertiary = GraphiteAccentDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    primaryContainer = SurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = GraphitePrimary,
    secondary = GraphiteSecondary,
    tertiary = GraphiteAccent,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    primaryContainer = SurfaceVariantLight
)

@Composable
fun VolunteerHelpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
