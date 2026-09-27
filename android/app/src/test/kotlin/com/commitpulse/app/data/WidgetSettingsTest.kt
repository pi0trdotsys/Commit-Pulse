package com.commitpulse.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSettingsTest {

    @Test
    fun `default settings use a valid built-in palette and a valid custom color`() {
        val settings = WidgetSettings()

        assertEquals(Palette.GITHUB, settings.palette)
        assertTrue("default customColorHex must itself be a valid hex", isValidHexColor(settings.customColorHex))
    }

    @Test
    fun `copying settings to the custom palette keeps the chosen color`() {
        val settings = WidgetSettings().copy(palette = Palette.CUSTOM, customColorHex = "#123ABC")

        assertEquals(Palette.CUSTOM, settings.palette)
        assertEquals("#123ABC", settings.customColorHex)
    }
}
