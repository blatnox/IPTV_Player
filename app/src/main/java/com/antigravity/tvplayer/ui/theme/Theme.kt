package com.antigravity.tvplayer.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val TvPlayerColorScheme = darkColorScheme(
    primary = FocusCyan,
    onPrimary = DarkBackground,
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = TextPrimary,
    secondary = AccentBlue,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = DarkBackground
)

@Composable
fun TvPlayerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvPlayerColorScheme,
        content = content
    )
}
