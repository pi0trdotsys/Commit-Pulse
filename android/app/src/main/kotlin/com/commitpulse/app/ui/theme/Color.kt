package com.commitpulse.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.commitpulse.app.data.DEFAULT_CUSTOM_COLOR_HEX
import com.commitpulse.app.data.Palette
import com.commitpulse.app.data.RgbColor
import com.commitpulse.app.data.WidgetSettings
import com.commitpulse.app.data.heatScale
import com.commitpulse.app.data.hexToRgb

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
)

private fun RgbColor.toComposeColor(): Color = Color(r, g, b)

/** Buduje paletę [Palette.CUSTOM] z pojedynczego koloru HEX podanego przez użytkownika. */
fun customPaletteFrom(hex: String): PaletteColors {
    val accent = hexToRgb(hex) ?: hexToRgb(DEFAULT_CUSTOM_COLOR_HEX)!!
    return PaletteColors(
        displayName = "Własny",
        heat = heatScale(accent).map { it.toComposeColor() },
        accent = accent.toComposeColor(),
    )
}

/** Rozwiązuje aktywną paletę: statyczną dla GITHUB/MONO, wygenerowaną z HEX-a dla CUSTOM. */
fun paletteFor(palette: Palette, customColorHex: String): PaletteColors = when (palette) {
    Palette.CUSTOM -> customPaletteFrom(customColorHex)
    else -> PALETTES.getValue(palette)
}

fun paletteFor(settings: WidgetSettings): PaletteColors = paletteFor(settings.palette, settings.customColorHex)
