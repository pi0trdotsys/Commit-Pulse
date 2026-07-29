package com.commitpulse.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF14203D),
    onPrimary = Color.White,
    background = Color.White,
    surface = Color.White,
    onBackground = Color(0xFF0B0E17),
    onSurface = Color(0xFF0B0E17),
    surfaceVariant = Color(0xFFF1F3F8),
    outline = Color(0xFFDCE0E8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE7EAF0),
    onPrimary = Color(0xFF14203D),
    background = Color(0xFF0B0E17),
    surface = Color(0xFF141925),
    onBackground = Color(0xFFF1F3F8),
    onSurface = Color(0xFFF1F3F8),
    surfaceVariant = Color(0xFF1E2430),
    outline = Color(0xFF2E3542),
)

@Composable
fun CommitPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
