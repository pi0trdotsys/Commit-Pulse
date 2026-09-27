package com.commitpulse.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.commitpulse.app.commitPulseApp
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.HeaderLayout
import com.commitpulse.app.data.Surface as WidgetSurface
import com.commitpulse.app.data.TapAction
import com.commitpulse.app.data.WeekDelta
import com.commitpulse.app.data.WidgetMode
import com.commitpulse.app.data.WidgetSettings
import com.commitpulse.app.data.asDateMap
import com.commitpulse.app.data.buildGridCells
import com.commitpulse.app.data.chooseHeaderLayout
import com.commitpulse.app.data.compactLabel
import com.commitpulse.app.data.computeGridLayout
import com.commitpulse.app.data.estimateTextWidthDp
import com.commitpulse.app.data.lastN
import com.commitpulse.app.data.lineHeightDp
import com.commitpulse.app.data.streak
import com.commitpulse.app.data.today
import com.commitpulse.app.data.weekOverWeek
import com.commitpulse.app.ui.MainActivity
import com.commitpulse.app.ui.theme.PaletteColors
import com.commitpulse.app.ui.theme.WidgetColors
import com.commitpulse.app.ui.theme.paletteFor
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val LABEL_SP = 8f
private const val DELTA_SP = 9f
private const val BADGE_SP = 9f
private const val ROW_GAP_DP = 2f
private const val SECTION_GAP_DP = 4f

class CommitPulseWidget : GlanceAppWidget() {

    /** Rzeczywisty rozmiar widgetu (nie zadeklarowane minimum) — cały układ liczony jest z niego. */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = context.commitPulseApp().settingsRepository
        val initialSettings = repository.currentSettings()
        val initialHistory = repository.historyFlow.first()

        provideContent {
            // Glance utrzymuje sesję widgetu i przy update() tylko rekomponuje treść, bez ponownego
            // provideGlance — dlatego stan musi być obserwowany tutaj, a nie odczytany raz wyżej.
            val settings by repository.settingsFlow.collectAsState(initialSettings)
            val history by repository.historyFlow.collectAsState(initialHistory)
            CommitPulseWidgetContent(settings, paletteFor(settings), history, history.lastN(settings.range.days))
        }
    }
}

/**
 * Wymiary obszaru treści + skala czcionki systemowej. Wszystkie decyzje o układzie (ile linii
 * nagłówka, orientacja siatki, rozmiar liczb) zapadają na podstawie tych liczb, żeby przy
 * żadnym rozmiarze widgetu ani ustawieniu czcionki tekst się nie zawijał i nic nie było ucinane.
 */
private class Metrics(widgetWidth: Dp, widgetHeight: Dp, val fontScale: Float, val density: Float) {
    val padding: Dp = if (widgetWidth < 80.dp || widgetHeight < 80.dp) 6.dp else 10.dp
    val width: Float = (widgetWidth - padding * 2).value.coerceAtLeast(1f)
    val height: Float = (widgetHeight - padding * 2).value.coerceAtLeast(1f)

    fun textWidth(text: String, sp: Float): Float = estimateTextWidthDp(text, sp, fontScale)
    fun lineHeight(sp: Float): Float = lineHeightDp(sp, fontScale)
    fun toPx(dp: Float): Int = (dp * density).roundToInt()

    /** Największa czcionka (sp), przy której [text] mieści się w [availableDp] szerokości. */
    fun spToFit(text: String, availableDp: Float): Float = availableDp / estimateTextWidthDp(text, 1f, fontScale)
}

@Composable
private fun rememberMetrics(): Metrics {
    val size = LocalSize.current
    val resources = LocalContext.current.resources
    return Metrics(size.width, size.height, resources.configuration.fontScale, resources.displayMetrics.density)
}

@Composable
private fun tapAction(action: TapAction): Action {
    val context = LocalContext.current
    return when (action) {
        TapAction.OPEN_APP -> actionStartActivity(Intent(context, MainActivity::class.java))
        TapAction.OPEN_PROFILE -> actionRunCallback<OpenProfileAction>()
        TapAction.REFRESH -> actionRunCallback<RefreshWidgetAction>()
    }
}

@Composable
private fun CommitPulseWidgetContent(
    settings: WidgetSettings,
    pal: PaletteColors,
    history: List<DayCommit>,
    rangeHistory: List<DayCommit>,
) {
    val bg = when (settings.surface) {
        WidgetSurface.TRANSPARENT -> ColorProvider(Color.Transparent)
        WidgetSurface.CARD -> ColorProvider(WidgetColors.surface.copy(alpha = 0.92f))
        WidgetSurface.DARK -> ColorProvider(WidgetColors.deep)
    }
    val m = rememberMetrics()

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bg)
            .cornerRadius(14.dp)
            .padding(m.padding)
            .clickable(tapAction(settings.tapAction)),
    ) {
        when (settings.mode) {
            WidgetMode.HEATMAP -> HeatmapMode(history, pal, m)
            WidgetMode.SPARKLINE -> SparklineMode(history, rangeHistory, pal, m)
            WidgetMode.COUNTER -> CounterMode(history, pal, m)
            WidgetMode.GOAL -> GoalMode(history, settings.goal, pal, m)
            WidgetMode.HEATMAP_30 -> Heatmap30Mode(history, pal, m)
        }
    }
}

// --- Wspólne elementy ---

private fun textStyle(sp: Float, color: Color, bold: Boolean = false) = TextStyle(
    fontSize = sp.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    color = ColorProvider(color),
)

@Composable
private fun OneLine(text: String, sp: Float, color: Color, bold: Boolean = false) {
    Text(text = text, style = textStyle(sp, color, bold), maxLines = 1)
}

/** Dwie warstwy Boxa wyrównane do lewej/prawej — zastępstwo braku Arrangement.SpaceBetween w Glance. */
@Composable
private fun SpaceBetweenRow(start: @Composable () -> Unit, end: @Composable () -> Unit) {
    Box(modifier = GlanceModifier.fillMaxWidth()) {
        Box(modifier = GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) { start() }
        Box(modifier = GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { end() }
    }
}

private fun Metrics.todayCountWidth(today: Int, sp: Float) = textWidth("$today", sp) + 3f + textWidth("dziś", LABEL_SP)

@Composable
private fun TodayCount(today: Int, sp: Float) {
    Row(verticalAlignment = Alignment.Vertical.Bottom) {
        OneLine("$today", sp, WidgetColors.fg, bold = true)
        Spacer(modifier = GlanceModifier.width(3.dp))
        OneLine("dziś", LABEL_SP, WidgetColors.muted)
    }
}

private fun streakBadgeWidth(m: Metrics, streak: Int) = m.textWidth("🔥", LABEL_SP) + 2f + m.textWidth("${streak}d", BADGE_SP) + 12f

@Composable
private fun StreakBadge(streak: Int, accent: Color) {
    Row(
        modifier = GlanceModifier
            .background(ColorProvider(accent.copy(alpha = 0.18f)))
            .cornerRadius(20.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        OneLine("🔥", LABEL_SP, accent)
        Spacer(modifier = GlanceModifier.width(2.dp))
        OneLine("${streak}d", BADGE_SP, accent, bold = true)
    }
}

private const val DELTA_SUFFIX = "vs poprz. 7 dni"

private fun deltaWidth(m: Metrics, wow: WeekDelta, withSuffix: Boolean): Float {
    val base = m.textWidth("▲", LABEL_SP) + 2f + m.textWidth(wow.compactLabel(), DELTA_SP)
    return if (withSuffix) base + 3f + m.textWidth(DELTA_SUFFIX, LABEL_SP) else base
}

@Composable
private fun DeltaRow(wow: WeekDelta, accent: Color, withSuffix: Boolean = false) {
    val color = when (wow.direction) {
        WeekDelta.Direction.UP -> accent
        WeekDelta.Direction.DOWN -> WidgetColors.trendDown
        WeekDelta.Direction.FLAT -> WidgetColors.muted
    }
    val arrow = when (wow.direction) {
        WeekDelta.Direction.UP -> "▲"
        WeekDelta.Direction.DOWN -> "▼"
        WeekDelta.Direction.FLAT -> "▬"
    }
    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        OneLine(arrow, LABEL_SP, color)
        Spacer(modifier = GlanceModifier.width(2.dp))
        OneLine(wow.compactLabel(), DELTA_SP, color, bold = true)
        if (withSuffix) {
            Spacer(modifier = GlanceModifier.width(3.dp))
            OneLine(DELTA_SUFFIX, LABEL_SP, WidgetColors.muted)
        }
    }
}

/** Siatka kontrybucji narysowana jako bitmapa w dokładnie takim rozmiarze, jaki się mieści. */
@Composable
private fun ContributionGridImage(
    history: List<DayCommit>,
    heat: List<Color>,
    m: Metrics,
    widthDp: Float,
    heightDp: Float,
    maxDays: Int,
    fitAllDays: Boolean,
) {
    val today = history.lastOrNull()?.date ?: LocalDate.now()
    val layout = computeGridLayout(widthDp, heightDp, maxDays, today, fitAllDays)
    val cells = buildGridCells(today, layout, earliest = history.firstOrNull()?.date)
    val counts = history.asDateMap()
    // Intensywność względem widocznego okresu, nie całej pobranej historii.
    val visibleMax = cells.flatten().maxOfOrNull { date -> date?.let { counts[it] } ?: 0 } ?: 0

    val gapPx = max(1, m.toPx(layout.gapDp))
    val availableW = floor(widthDp * m.density).toInt()
    val availableH = floor(heightDp * m.density).toInt()
    val cellFitW = (availableW - (layout.columns - 1) * gapPx) / layout.columns
    val cellFitH = (availableH - (layout.rows - 1) * gapPx) / layout.rows
    val cellPx = min(m.toPx(layout.cellDp), min(cellFitW, cellFitH)).coerceAtLeast(2)

    val bitmap = WidgetGraphics.contributionGrid(cells, counts, visibleMax, heat, cellPx, gapPx)
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = GlanceModifier.size((bitmap.width / m.density).dp, (bitmap.height / m.density).dp),
    )
}

// --- Tryby ---

private class StatsHeaderSpec(val layout: HeaderLayout, val countSp: Float, val heightDp: Float)

private fun statsHeaderSpec(m: Metrics, today: Int, wow: WeekDelta, streak: Int): StatsHeaderSpec {
    val countSp = if (m.width >= 220f) 20f else 16f
    val layout = chooseHeaderLayout(
        widthDp = m.width,
        countWidthDp = m.todayCountWidth(today, countSp),
        deltaWidthDp = deltaWidth(m, wow, withSuffix = false),
        streakWidthDp = streakBadgeWidth(m, streak),
    )
    val count = m.lineHeight(countSp)
    val delta = m.lineHeight(DELTA_SP)
    val badge = m.lineHeight(BADGE_SP) + 4f
    val height = when (layout) {
        HeaderLayout.ONE_ROW -> max(count, badge)
        HeaderLayout.TWO_ROWS -> max(count, badge) + ROW_GAP_DP + delta
        HeaderLayout.THREE_ROWS -> count + ROW_GAP_DP + delta + ROW_GAP_DP + badge
    }
    return StatsHeaderSpec(layout, countSp, height)
}

@Composable
private fun StatsHeader(spec: StatsHeaderSpec, today: Int, wow: WeekDelta, streak: Int, accent: Color) {
    when (spec.layout) {
        HeaderLayout.ONE_ROW -> SpaceBetweenRow(
            start = {
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    TodayCount(today, spec.countSp)
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    DeltaRow(wow, accent)
                }
            },
            end = { StreakBadge(streak, accent) },
        )
        HeaderLayout.TWO_ROWS -> Column {
            SpaceBetweenRow(start = { TodayCount(today, spec.countSp) }, end = { StreakBadge(streak, accent) })
            Spacer(modifier = GlanceModifier.height(ROW_GAP_DP.dp))
            DeltaRow(wow, accent)
        }
        HeaderLayout.THREE_ROWS -> Column {
            TodayCount(today, spec.countSp)
            Spacer(modifier = GlanceModifier.height(ROW_GAP_DP.dp))
            DeltaRow(wow, accent)
            Spacer(modifier = GlanceModifier.height(ROW_GAP_DP.dp))
            StreakBadge(streak, accent)
        }
    }
}

@Composable
private fun HeatmapMode(history: List<DayCommit>, pal: PaletteColors, m: Metrics) {
    val today = history.today()
    val wow = history.weekOverWeek()
    val streak = history.streak()
    val header = statsHeaderSpec(m, today, wow, streak)
    // Bardzo niski widget: nagłówek zabrałby całe miejsce siatce, więc zostaje sama siatka.
    val showHeader = m.height - header.heightDp - SECTION_GAP_DP >= 24f
    val gridHeight = if (showHeader) m.height - header.heightDp - SECTION_GAP_DP else m.height

    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        if (showHeader) {
            StatsHeader(header, today, wow, streak, pal.accent)
            Spacer(modifier = GlanceModifier.height(SECTION_GAP_DP.dp))
        }
        ContributionGridImage(history, pal.heat, m, m.width, gridHeight, maxDays = history.size.coerceAtLeast(7), fitAllDays = false)
    }
}

@Composable
private fun Heatmap30Mode(history: List<DayCommit>, pal: PaletteColors, m: Metrics) {
    val wow = history.weekOverWeek()
    val title = "30 DNI"
    val oneRow = m.textWidth(title, LABEL_SP) + 8f + deltaWidth(m, wow, withSuffix = false) <= m.width
    val headerHeight = if (oneRow) m.lineHeight(DELTA_SP) else m.lineHeight(LABEL_SP) + ROW_GAP_DP + m.lineHeight(DELTA_SP)
    val showHeader = m.height - headerHeight - SECTION_GAP_DP >= 24f
    val gridHeight = if (showHeader) m.height - headerHeight - SECTION_GAP_DP else m.height

    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        if (showHeader) {
            if (oneRow) {
                SpaceBetweenRow(
                    start = { OneLine(title, LABEL_SP, WidgetColors.muted) },
                    end = { DeltaRow(wow, pal.accent) },
                )
            } else {
                Column {
                    OneLine(title, LABEL_SP, WidgetColors.muted)
                    Spacer(modifier = GlanceModifier.height(ROW_GAP_DP.dp))
                    DeltaRow(wow, pal.accent)
                }
            }
            Spacer(modifier = GlanceModifier.height(SECTION_GAP_DP.dp))
        }
        // Pełna historia jako źródło liczników: siatka domyka pełne tygodnie prawdziwymi danymi,
        // zamiast poszarpanej pierwszej kolumny ucinanej dokładnie na 30. dniu.
        ContributionGridImage(history, pal.heat, m, m.width, gridHeight, maxDays = 30, fitAllDays = true)
    }
}

@Composable
private fun SparklineMode(history: List<DayCommit>, rangeHistory: List<DayCommit>, pal: PaletteColors, m: Metrics) {
    val today = history.today()
    val wow = history.weekOverWeek()
    val countCap = if (m.width >= 220f || m.height >= 140f) 28f else 22f
    val countSp = min(countCap, m.spToFit("$today", m.width * 0.6f))
    val countWidth = m.todayCountWidth(today, countSp)
    val oneRow = countWidth + 8f + deltaWidth(m, wow, withSuffix = false) <= m.width
    val headerHeight = if (oneRow) m.lineHeight(countSp) else m.lineHeight(countSp) + ROW_GAP_DP + m.lineHeight(DELTA_SP)
    // Wykres nie wyższy niż ~0,7 szerokości — w wąskim, wysokim widgecie krzywa rozciągnięta
    // na całą wysokość robi się nieczytelnie szpiczasta.
    val chartHeight = min(m.height - headerHeight - SECTION_GAP_DP, max(m.width * 0.7f, 28f)).coerceAtLeast(12f)
    val bitmap = WidgetGraphics.sparkline(rangeHistory, pal.accent, m.toPx(m.width), m.toPx(chartHeight), m.density)

    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        if (oneRow) {
            SpaceBetweenRow(start = { TodayCount(today, countSp) }, end = { DeltaRow(wow, pal.accent) })
        } else {
            Column {
                TodayCount(today, countSp)
                Spacer(modifier = GlanceModifier.height(ROW_GAP_DP.dp))
                DeltaRow(wow, pal.accent)
            }
        }
        Spacer(modifier = GlanceModifier.height(SECTION_GAP_DP.dp))
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.size(m.width.dp, chartHeight.dp),
        )
    }
}

@Composable
private fun CounterMode(history: List<DayCommit>, pal: PaletteColors, m: Metrics) {
    val number = history.today().toString()
    val wow = history.weekOverWeek()
    val withSuffix = deltaWidth(m, wow, withSuffix = true) <= m.width
    val labelWidth = m.textWidth("commitów", LABEL_SP)
    val labels = 2 * m.lineHeight(LABEL_SP)
    val footer = m.lineHeight(DELTA_SP) + SECTION_GAP_DP

    // Etykieta obok liczby, jeśli liczba zostaje czytelna (≥ 24sp); w przeciwnym razie pod nią.
    val sideBySp = min(44f, m.spToFit(number, m.width - 6f - labelWidth))
    val sideBySide = sideBySp >= 24f
    val maxByHeight = (m.height - footer - if (sideBySide) 0f else labels) / (1.3f * m.fontScale)
    val numberSp = if (sideBySide) {
        min(sideBySp, maxByHeight)
    } else {
        min(min(44f, m.spToFit(number, m.width)), maxByHeight)
    }.coerceAtLeast(12f)

    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        if (sideBySide) {
            Row(verticalAlignment = Alignment.Vertical.Bottom) {
                OneLine(number, numberSp, WidgetColors.fg, bold = true)
                Spacer(modifier = GlanceModifier.width(6.dp))
                Column {
                    OneLine("commitów", LABEL_SP, WidgetColors.muted)
                    OneLine("dzisiaj", LABEL_SP, WidgetColors.muted)
                }
            }
        } else {
            OneLine(number, numberSp, WidgetColors.fg, bold = true)
            OneLine("commitów", LABEL_SP, WidgetColors.muted)
            OneLine("dzisiaj", LABEL_SP, WidgetColors.muted)
        }
        Spacer(modifier = GlanceModifier.height(SECTION_GAP_DP.dp))
        DeltaRow(wow, pal.accent, withSuffix = withSuffix)
    }
}

@Composable
private fun GoalMode(history: List<DayCommit>, goal: Int, pal: PaletteColors, m: Metrics) {
    val today = history.today()
    val wow = history.weekOverWeek()
    val streak = history.streak()

    val goalText = "cel $goal/dzień".takeIf { m.textWidth(it, 10f) <= m.width } ?: "cel $goal"
    val streakText = "🔥 seria $streak dni".takeIf { m.textWidth(it, LABEL_SP) <= m.width } ?: "🔥 $streak dni"
    val infoWidth = maxOf(m.textWidth(goalText, 10f), deltaWidth(m, wow, withSuffix = false), m.textWidth(streakText, LABEL_SP))
    val infoHeight = m.lineHeight(10f) + ROW_GAP_DP + m.lineHeight(DELTA_SP) + ROW_GAP_DP + m.lineHeight(LABEL_SP)

    val rowRing = min(min(m.height, 72f), m.width - 8f - infoWidth)
    val useRow = rowRing >= 36f
    val ringDp = if (useRow) rowRing else min(min(m.width, 72f), m.height - infoHeight - 6f).coerceAtLeast(24f)
    val ringBitmap = WidgetGraphics.goalRing(today, goal, pal.accent, m.toPx(ringDp))
    val ringNumberSp = min(ringDp * 0.34f / m.fontScale, m.spToFit("$today", ringDp * 0.6f))

    val ring: @Composable () -> Unit = {
        Box(modifier = GlanceModifier.size(ringDp.dp), contentAlignment = Alignment.Center) {
            Image(provider = ImageProvider(ringBitmap), contentDescription = null, modifier = GlanceModifier.size(ringDp.dp))
            OneLine("$today", ringNumberSp, WidgetColors.fg, bold = true)
        }
    }
    val info: @Composable () -> Unit = {
        Column(horizontalAlignment = if (useRow) Alignment.Horizontal.Start else Alignment.Horizontal.CenterHorizontally) {
            OneLine(goalText, 10f, WidgetColors.fg, bold = true)
            Spacer(modifier = GlanceModifier.height(ROW_GAP_DP.dp))
            DeltaRow(wow, pal.accent)
            Spacer(modifier = GlanceModifier.height(ROW_GAP_DP.dp))
            OneLine(streakText, LABEL_SP, WidgetColors.muted)
        }
    }

    if (useRow) {
        Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            ring()
            Spacer(modifier = GlanceModifier.width(8.dp))
            info()
        }
    } else {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            ring()
            Spacer(modifier = GlanceModifier.height(6.dp))
            info()
        }
    }
}
