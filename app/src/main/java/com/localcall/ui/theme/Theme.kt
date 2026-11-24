package com.localcall.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object LocalCallColors {
    val Background = Color(0xFF0B1020)
    val Surface = Color(0xFF141B2F)
    val SurfaceHigh = Color(0xFF1E2742)
    val Accent = Color(0xFF2DD4BF)
    val OnAccent = Color(0xFF04201C)
    val Danger = Color(0xFFEF4444)
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
}

private val Scheme = darkColorScheme(
    primary = LocalCallColors.Accent,
    onPrimary = LocalCallColors.OnAccent,
    background = LocalCallColors.Background,
    onBackground = LocalCallColors.TextPrimary,
    surface = LocalCallColors.Surface,
    onSurface = LocalCallColors.TextPrimary,
    surfaceVariant = LocalCallColors.SurfaceHigh,
    onSurfaceVariant = LocalCallColors.TextSecondary,
    error = LocalCallColors.Danger,
)

@Composable
fun LocalCallTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
