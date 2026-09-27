package com.commitpulse.app.data

/** Domyślny kolor własnej palety, dopóki użytkownik nie wpisze własnego HEX-a. */
const val DEFAULT_CUSTOM_COLOR_HEX = "#F0883E"

data class RgbColor(val r: Int, val g: Int, val b: Int)

private val HEX_DIGITS = Regex("^[0-9a-fA-F]+$")

/**
 * Normalizuje wpisany kolor do postaci "#RRGGBB" (wielkie litery).
 * Akceptuje "#RGB", "RGB", "#RRGGBB" i "RRGGBB". Zwraca null dla nieprawidłowego wejścia.
 */
fun normalizeHexColor(input: String): String? {
    val cleaned = input.trim().removePrefix("#")
    val expanded = when (cleaned.length) {
        3 -> cleaned.map { "$it$it" }.joinToString("")
        6 -> cleaned
        else -> return null
    }
    if (!HEX_DIGITS.matches(expanded)) return null
    return "#" + expanded.uppercase()
}

fun isValidHexColor(input: String): Boolean = normalizeHexColor(input) != null

fun hexToRgb(hex: String): RgbColor? {
    val normalized = normalizeHexColor(hex) ?: return null
    val value = normalized.removePrefix("#")
    return RgbColor(
        r = value.substring(0, 2).toInt(16),
        g = value.substring(2, 4).toInt(16),
        b = value.substring(4, 6).toInt(16),
    )
}

fun RgbColor.toHex(): String = "#%02X%02X%02X".format(r, g, b)

data class Hsv(val h: Float, val s: Float, val v: Float)

fun rgbToHsv(c: RgbColor): Hsv {
    val r = c.r / 255f
    val g = c.g / 255f
    val b = c.b / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val h = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta).mod(6f))
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    val s = if (max == 0f) 0f else delta / max
    return Hsv(h, s, max)
}

fun hsvToRgb(hsv: Hsv): RgbColor {
    val c = hsv.v * hsv.s
    val x = c * (1 - kotlin.math.abs((hsv.h / 60f).mod(2f) - 1))
    val m = hsv.v - c
    val (r1, g1, b1) = when {
        hsv.h < 60f -> Triple(c, x, 0f)
        hsv.h < 120f -> Triple(x, c, 0f)
        hsv.h < 180f -> Triple(0f, c, x)
        hsv.h < 240f -> Triple(0f, x, c)
        hsv.h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return RgbColor(
        r = (((r1 + m) * 255f).toInt()).coerceIn(0, 255),
        g = (((g1 + m) * 255f).toInt()).coerceIn(0, 255),
        b = (((b1 + m) * 255f).toInt()).coerceIn(0, 255),
    )
}

/** Punkty jasności (0=tło..1=pełna jasność) użyte do generowania skali heatmapy z jednego koloru. */
val HEAT_SCALE_STEPS = listOf(0.14f, 0.32f, 0.52f, 0.72f, 0.92f)

/**
 * Generuje 5-stopniową skalę cieplną w stylu heatmapy GitHuba z pojedynczego koloru akcentu:
 * ten sam odcień (hue), rosnąca jasność i lekko rosnące nasycenie od tła do pełnej intensywności.
 */
fun heatScale(accent: RgbColor, steps: List<Float> = HEAT_SCALE_STEPS): List<RgbColor> {
    val hsv = rgbToHsv(accent)
    return steps.map { v ->
        hsvToRgb(hsv.copy(s = (hsv.s * (0.4f + 0.6f * v)).coerceIn(0f, 1f), v = v))
    }
}
