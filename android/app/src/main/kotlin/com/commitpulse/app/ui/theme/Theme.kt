package com.commitpulse.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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
    outline = Color(0xFF5A6272),
    outlineVariant = Color(0xFF2E3542),
)

@Composable
fun CommitPulseTheme(
    materialYou: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        materialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
