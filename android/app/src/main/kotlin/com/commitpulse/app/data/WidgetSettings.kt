package com.commitpulse.app.data

enum class WidgetMode { HEATMAP, SPARKLINE, COUNTER, GOAL, HEATMAP_30 }

enum class Palette { GITHUB, MONO, CUSTOM }

enum class Surface { TRANSPARENT, CARD, DARK }

enum class Range(val days: Int) { SEVEN(7), FOURTEEN(14), THIRTY(30) }

enum class RefreshInterval(val minutes: Long) { FIFTEEN_MIN(15), ONE_HOUR(60), SIX_HOURS(360) }

enum class TapAction { OPEN_APP, OPEN_PROFILE, REFRESH }

/** Jak pokazywać zmianę tydzień do tygodnia: różnica kontrybucji (+28) albo procent (+467%). */
enum class TrendFormat { DIFF, PERCENT }

/** Liczba tygodni na heatmapie; 0 = automatycznie, tyle ile zmieści się w rozmiarze widgetu. */
val GRID_WEEK_OPTIONS = listOf(0, 4, 8, 12, 16, 26, 52)

data class WidgetSettings(
    val mode: WidgetMode = WidgetMode.HEATMAP,
    val palette: Palette = Palette.GITHUB,
    val customColorHex: String = DEFAULT_CUSTOM_COLOR_HEX,
    val materialYou: Boolean = false,
    val minimal: Boolean = false,
    val gridWeeks: Int = 0,
    val trendFormat: TrendFormat = TrendFormat.DIFF,
    val surface: Surface = Surface.CARD,
    val goal: Int = 8,
    val range: Range = Range.FOURTEEN,
    val refresh: RefreshInterval = RefreshInterval.ONE_HOUR,
    val tapAction: TapAction = TapAction.OPEN_APP,
    val weeklyDigest: Boolean = true,
    val digestDay: Int = 1, // 1=Mon..7=Sun (ISO)
    val digestHour: String = "09:00",
    val alertStreak: Boolean = true,
    val alertGoal: Boolean = false,
    val alertMilestones: Boolean = true,
)

data class ModeInfo(val id: WidgetMode, val nameRes: String, val descRes: String)

val MODES = listOf(
    ModeInfo(WidgetMode.HEATMAP, "Heatmapa + liczba", "Siatka tygodni, licznik dnia, seria — rekomendowany"),
    ModeInfo(WidgetMode.SPARKLINE, "Sparkline", "Krzywa z wybranego zakresu dni + licznik dnia"),
    ModeInfo(WidgetMode.COUNTER, "Licznik", "Duża liczba i zmiana vs poprzednie 7 dni"),
    ModeInfo(WidgetMode.GOAL, "Pierścień celu", "Postęp do celu dziennego"),
    ModeInfo(WidgetMode.HEATMAP_30, "Heatmapa 30 dni", "Kompaktowa, sam rytm pracy"),
)
