package com.commitpulse.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.commitpulse.app.data.Palette

/**
 * Wartości sRGB przeliczone z tokenów oklch z src/styles.css i src/lib/widget-settings.ts
 * (web makieta), żeby zachować identyczny wygląd w natywnej aplikacji.
 */
object WidgetColors {
    val fg = Color(0xFFF3F5F8)
    val muted = Color(0xFF9399A0)
    val surface = Color(0xFF1B1F27)
    val deep = Color(0xFF080B10)
    val line = Color(0x1FFFFFFF)

    val trendUp = Color(0xFF37D880)
    val trendDown = Color(0xFFF75D59)

    val bgGradientTop = Color(0xFF154470)
    val bgGradientBottom = Color(0xFF005932)
    val bgGradientDark1 = Color(0xFF0F141D)
    val bgGradientDark2 = Color(0xFF05070B)
}

data class PaletteColors(
    val displayName: String,
    val heat: List<Color>,
    val accent: Color,
)

val PALETTES: Map<Palette, PaletteColors> = mapOf(
    Palette.GITHUB to PaletteColors(
        displayName = "GitHub green",
        heat = listOf(
            Color(0xFF202C25),
            Color(0xFF1D6442),
            Color(0xFF1B915A),
            Color(0xFF0EC073),
            Color(0xFF4FED8A),
        ),
        accent = Color(0xFF37D880),
    ),
    Palette.MONO to PaletteColors(
        displayName = "Mono",
        heat = listOf(
            Color(0xFF27292B),
            Color(0xFF4B4D50),
            Color(0xFF727577),
            Color(0xFFA2A5A8),
            Color(0xFFE5E8EB),
        ),
        accent = Color(0xFFE5E8EB),
    ),
    Palette.CUSTOM to PaletteColors(
        displayName = "Custom / amber",
        heat = listOf(
            Color(0xFF30271F),
            Color(0xFF7E4E1E),
            Color(0xFFBE7200),
            Color(0xFFF09C17),
            Color(0xFFFFC72B),
        ),
        accent = Color(0xFFFFB51A),
    ),
)
