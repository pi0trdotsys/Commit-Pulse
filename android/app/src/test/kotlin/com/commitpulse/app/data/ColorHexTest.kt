package com.commitpulse.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorHexTest {

    @Test
    fun `normalizeHexColor expands a 3-digit shorthand and uppercases it`() {
        assertEquals("#33AAFF", normalizeHexColor("#3af"))
    }

    @Test
    fun `normalizeHexColor accepts 6-digit hex with or without the hash`() {
        assertEquals("#37D880", normalizeHexColor("#37d880"))
        assertEquals("#37D880", normalizeHexColor("37d880"))
    }

    @Test
    fun `normalizeHexColor trims surrounding whitespace`() {
        assertEquals("#37D880", normalizeHexColor("  #37d880  "))
    }

    @Test
    fun `normalizeHexColor rejects invalid lengths and characters`() {
        assertNull(normalizeHexColor("#12345"))
        assertNull(normalizeHexColor("#1234567"))
        assertNull(normalizeHexColor("#GGHHII"))
        assertNull(normalizeHexColor(""))
    }

    @Test
    fun `isValidHexColor mirrors normalizeHexColor`() {
        assertTrue(isValidHexColor("#37D880"))
        assertFalse(isValidHexColor("not-a-color"))
    }

    @Test
    fun `hexToRgb decodes each channel`() {
        val rgb = hexToRgb("#37D880")
        assertEquals(RgbColor(0x37, 0xD8, 0x80), rgb)
    }

    @Test
    fun `hexToRgb returns null for invalid input`() {
        assertNull(hexToRgb("nope"))
    }

    @Test
    fun `toHex round-trips through hexToRgb`() {
        val original = "#F0883E"
        val rgb = hexToRgb(original)!!
        assertEquals(original, rgb.toHex())
    }

    @Test
    fun `rgbToHsv and hsvToRgb round-trip within rounding error`() {
        val original = RgbColor(0x37, 0xD8, 0x80)
        val roundTripped = hsvToRgb(rgbToHsv(original))
        assertTrue(kotlin.math.abs(original.r - roundTripped.r) <= 1)
        assertTrue(kotlin.math.abs(original.g - roundTripped.g) <= 1)
        assertTrue(kotlin.math.abs(original.b - roundTripped.b) <= 1)
    }

    @Test
    fun `heatScale returns one color per requested step`() {
        val accent = hexToRgb("#37D880")!!
        val scale = heatScale(accent, listOf(0f, 0.5f, 1f))
        assertEquals(3, scale.size)
    }

    @Test
    fun `heatScale brightness increases monotonically with the step value`() {
        val accent = hexToRgb(DEFAULT_CUSTOM_COLOR_HEX)!!
        val scale = heatScale(accent)
        val brightness = scale.map { it.r + it.g + it.b }
        for (i in 1 until brightness.size) {
            assertTrue(
                "step $i (${brightness[i]}) should be brighter than step ${i - 1} (${brightness[i - 1]})",
                brightness[i] > brightness[i - 1],
            )
        }
    }

    @Test
    fun `heatScale preserves the accent's hue`() {
        val accent = hexToRgb("#37D880")!!
        val accentHue = rgbToHsv(accent).h
        val scale = heatScale(accent)
        scale.forEach { step ->
            val hue = rgbToHsv(step).h
            assertTrue(kotlin.math.abs(hue - accentHue) < 1f)
        }
    }
}
