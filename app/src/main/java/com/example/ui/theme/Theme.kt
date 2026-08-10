package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SleekPrimaryContainer,
    onPrimary = SleekOnPrimaryContainer,
    primaryContainer = SleekPrimary,
    onPrimaryContainer = SleekPrimaryContainer,
    secondary = SleekSecondaryContainer,
    onSecondary = SleekOnSecondaryContainer,
    secondaryContainer = SleekSecondary,
    onSecondaryContainer = SleekSecondaryContainer,
    tertiary = SleekTertiaryContainer,
    onTertiary = SleekOnTertiaryContainer,
    background = SleekOnBackground,
    surface = SleekOnBackground,
    onBackground = SleekBackground,
    onSurface = SleekBackground,
    surfaceVariant = SleekOnSurfaceVariant,
    onSurfaceVariant = SleekSurfaceVariant,
    outline = SleekOutline,
    outlineVariant = SleekOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = SleekPrimary,
    onPrimary = SleekOnPrimary,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = SleekOnPrimaryContainer,
    secondary = SleekSecondary,
    onSecondary = SleekOnSecondary,
    secondaryContainer = SleekSecondaryContainer,
    onSecondaryContainer = SleekOnSecondaryContainer,
    tertiary = SleekTertiary,
    onTertiary = SleekOnTertiary,
    tertiaryContainer = SleekTertiaryContainer,
    onTertiaryContainer = SleekOnTertiaryContainer,
    error = SleekError,
    onError = SleekOnError,
    errorContainer = SleekErrorContainer,
    onErrorContainer = SleekOnErrorContainer,
    background = SleekBackground,
    onBackground = SleekOnBackground,
    surface = SleekSurface,
    onSurface = SleekOnSurface,
    surfaceVariant = SleekSurfaceVariant,
    onSurfaceVariant = SleekOnSurfaceVariant,
    outline = SleekOutline,
    outlineVariant = SleekOutlineVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Always force clean, high-contrast light theme as requested
    content: @Composable () -> Unit,
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
