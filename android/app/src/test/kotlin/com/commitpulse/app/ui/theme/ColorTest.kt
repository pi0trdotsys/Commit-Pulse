package com.commitpulse.app.ui.theme

import com.commitpulse.app.data.DEFAULT_CUSTOM_COLOR_HEX
import com.commitpulse.app.data.Palette
import com.commitpulse.app.data.WidgetSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ColorTest {

    @Test
    fun `paletteFor returns the static palette for GITHUB and MONO`() {
        assertEquals(PALETTES.getValue(Palette.GITHUB), paletteFor(Palette.GITHUB, "#000000"))
        assertEquals(PALETTES.getValue(Palette.MONO), paletteFor(Palette.MONO, "#000000"))
    }

    @Test
    fun `paletteFor builds a custom palette from the given hex for CUSTOM`() {
        val red = customPaletteFrom("#FF0000")
        val blue = customPaletteFrom("#0000FF")

        assertEquals("Własny", red.displayName)
        assertNotEquals(red.accent, blue.accent)
        assertEquals(5, red.heat.size)
    }

    @Test
    fun `customPaletteFrom falls back to the default color for invalid hex`() {
        val fallback = customPaletteFrom("not-a-hex")
        val default = customPaletteFrom(DEFAULT_CUSTOM_COLOR_HEX)

        assertEquals(default.accent, fallback.accent)
    }

    @Test
    fun `paletteFor(settings) resolves through the settings' own palette and hex`() {
        val settings = WidgetSettings(palette = Palette.CUSTOM, customColorHex = "#37D880")

        assertEquals(customPaletteFrom("#37D880"), paletteFor(settings))
    }
}
