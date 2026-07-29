package com.commitpulse.app.data

enum class WidgetMode { HEATMAP, SPARKLINE, COUNTER, GOAL, HEATMAP_30 }

enum class Palette { GITHUB, MONO, CUSTOM }

enum class Surface { TRANSPARENT, CARD, DARK }

enum class Range(val days: Int) { SEVEN(7), FOURTEEN(14), THIRTY(30) }

enum class RefreshInterval(val minutes: Long) { FIFTEEN_MIN(15), ONE_HOUR(60), SIX_HOURS(360) }

enum class TapAction { OPEN_APP, OPEN_PROFILE, REFRESH }

data class WidgetSettings(
    val mode: WidgetMode = WidgetMode.HEATMAP,
    val palette: Palette = Palette.GITHUB,
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
)

data class ModeInfo(val id: WidgetMode, val nameRes: String, val descRes: String)

val MODES = listOf(
    ModeInfo(WidgetMode.HEATMAP, "Heatmapa + liczba", "14 dni, licznik dnia, streak — rekomendowany"),
    ModeInfo(WidgetMode.SPARKLINE, "Sparkline", "Krzywa 14 dni + licznik dnia"),
    ModeInfo(WidgetMode.COUNTER, "Licznik", "Duża liczba i delta tydzień/tydzień"),
    ModeInfo(WidgetMode.GOAL, "Pierścień celu", "Postęp do celu dziennego"),
    ModeInfo(WidgetMode.HEATMAP_30, "Heatmapa 30 dni", "Kompaktowa, sam rytm pracy"),
)
