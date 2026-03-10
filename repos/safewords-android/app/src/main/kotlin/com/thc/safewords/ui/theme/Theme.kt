package com.thc.safewords.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Teal,
    onPrimary = Background,
    primaryContainer = TealDark,
    onPrimaryContainer = Teal,
    secondary = Amber,
    onSecondary = Background,
    secondaryContainer = Amber,
    onSecondaryContainer = Background,
    tertiary = TealMuted,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = Error,
    onError = TextPrimary,
    outline = TextSubtle,
    outlineVariant = SurfaceBright
)

@Composable
fun SafewordsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
