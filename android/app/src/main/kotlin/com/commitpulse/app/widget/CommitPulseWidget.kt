package com.commitpulse.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
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
import com.commitpulse.app.data.CommitSummary
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.HeaderLayout
import com.commitpulse.app.data.Surface as WidgetSurface
import com.commitpulse.app.data.TapAction
import com.commitpulse.app.data.Trend
import com.commitpulse.app.data.TrendDirection
import com.commitpulse.app.data.TrendFormat
import com.commitpulse.app.data.WidgetMode
import com.commitpulse.app.data.WidgetSettings
import com.commitpulse.app.data.asDateMap
import com.commitpulse.app.data.buildGridCells
import com.commitpulse.app.data.chooseHeaderLayout
import com.commitpulse.app.data.computeGridLayout
import com.commitpulse.app.data.estimateTextWidthDp
import com.commitpulse.app.data.lastN
import com.commitpulse.app.data.lineHeightDp
import com.commitpulse.app.data.summarize
import com.commitpulse.app.ui.MainActivity
import com.commitpulse.app.ui.theme.WidgetColors
import com.commitpulse.app.ui.theme.WidgetTheme
import com.commitpulse.app.ui.theme.widgetTheme
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

private val LocalWidgetTheme = staticCompositionLocalOf {
    WidgetTheme(WidgetColors.fg, WidgetColors.muted, WidgetColors.surface, WidgetColors.deep, WidgetColors.trendUp, emptyList(), WidgetColors.trendDown)
}

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
            CommitPulseWidgetContent(settings, history, LocalDate.now(), widgetTheme(context, settings))
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

/** Cała treść widgetu; [interactive] = false dla podglądu w aplikacji (bez akcji tapnięcia). */
@Composable
internal fun CommitPulseWidgetContent(
    settings: WidgetSettings,
    history: List<DayCommit>,
    today: LocalDate,
    theme: WidgetTheme,
    interactive: Boolean = true,
) {
    val bg = when (settings.surface) {
        WidgetSurface.TRANSPARENT -> ColorProvider(Color.Transparent)
        WidgetSurface.CARD -> ColorProvider(theme.card.copy(alpha = 0.92f))
        WidgetSurface.DARK -> ColorProvider(theme.deep)
    }
    val m = rememberMetrics()
    val summary = summarize(history, today)
    val base = GlanceModifier.fillMaxSize().background(bg).cornerRadius(14.dp).padding(m.padding)

    CompositionLocalProvider(LocalWidgetTheme provides theme) {
        Box(modifier = if (interactive) base.clickable(tapAction(settings.tapAction)) else base) {
            when (settings.mode) {
                WidgetMode.HEATMAP -> HeatmapMode(history, summary, settings, m)
                WidgetMode.SPARKLINE -> SparklineMode(history.lastN(settings.range.days), summary, settings, m)
                WidgetMode.COUNTER -> CounterMode(summary, settings, m)
                WidgetMode.GOAL -> GoalMode(summary, settings, m)
                WidgetMode.HEATMAP_30 -> Heatmap30Mode(history, summary, settings, m)
            }
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
    val theme = LocalWidgetTheme.current
    Row(verticalAlignment = Alignment.Vertical.Bottom) {
        OneLine("$today", sp, theme.fg, bold = true)
        Spacer(modifier = GlanceModifier.width(3.dp))
        OneLine("dziś", LABEL_SP, theme.muted)
    }
}

private fun streakBadgeWidth(m: Metrics, streak: Int) = m.textWidth("🔥", LABEL_SP) + 2f + m.textWidth("${streak}d", BADGE_SP) + 12f

@Composable
private fun StreakBadge(streak: Int) {
    val accent = LocalWidgetTheme.current.accent
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

private const val TREND_SUFFIX = "vs poprz. 7 dni"

private fun trendWidth(m: Metrics, trend: Trend, format: TrendFormat, withSuffix: Boolean): Float {
    val base = m.textWidth("▲", LABEL_SP) + 2f + m.textWidth(trend.label(format), DELTA_SP)
    return if (withSuffix) base + 3f + m.textWidth(TREND_SUFFIX, LABEL_SP) else base
}

/** Zmiana ostatnich 7 dni względem 7 dni wcześniej (wyjaśnienie w aplikacji, zakładka Podgląd). */
@Composable
private fun TrendRow(trend: Trend, format: TrendFormat, withSuffix: Boolean = false) {
    val theme = LocalWidgetTheme.current
    val color = when (trend.direction) {
        TrendDirection.UP -> theme.accent
        TrendDirection.DOWN -> theme.down
        TrendDirection.FLAT -> theme.muted
    }
    val arrow = when (trend.direction) {
        TrendDirection.UP -> "▲"
        TrendDirection.DOWN -> "▼"
        TrendDirection.FLAT -> "▬"
    }
    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        OneLine(arrow, LABEL_SP, color)
        Spacer(modifier = GlanceModifier.width(2.dp))
        OneLine(trend.label(format), DELTA_SP, color, bold = true)
        if (withSuffix) {
            Spacer(modifier = GlanceModifier.width(3.dp))
            OneLine(TREND_SUFFIX, LABEL_SP, theme.muted)
        }
    }
}

/** Siatka kontrybucji narysowana jako bitmapa w dokładnie takim rozmiarze, jaki się mieści. */
@Composable
private fun ContributionGridImage(
    history: List<DayCommit>,
    today: LocalDate,
    m: Metrics,
    widthDp: Float,
    heightDp: Float,
    maxDays: Int,
    fitAllDays: Boolean,
    fixedWeeks: Int = 0,
) {
    val layout = computeGridLayout(widthDp, heightDp, maxDays, today, fitAllDays, fixedWeeks)
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

    val bitmap = WidgetGraphics.contributionGrid(cells, counts, visibleMax, LocalWidgetTheme.current.heat, cellPx, gapPx)
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = GlanceModifier.size((bitmap.width / m.density).dp, (bitmap.height / m.density).dp),
    )
}

// --- Tryby ---

private class StatsHeaderSpec(val layout: HeaderLayout, val countSp: Float, val heightDp: Float)

private fun statsHeaderSpec(m: Metrics, s: CommitSummary, format: TrendFormat): StatsHeaderSpec {
    val countSp = if (m.width >= 220f) 20f else 16f
    val layout = chooseHeaderLayout(
        widthDp = m.width,
        countWidthDp = m.todayCountWidth(s.todayCount, countSp),
        deltaWidthDp = trendWidth(m, s.trend, format, withSuffix = false),
        streakWidthDp = streakBadgeWidth(m, s.streak),
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
private fun StatsHeader(spec: StatsHeaderSpec, s: CommitSummary, format: TrendFormat) {
    when (spec.layout) {
        HeaderLayout.ONE_ROW -> SpaceBetweenRow(
            start = {
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    TodayCount(s.todayCount, spec.countSp)
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    TrendRow(s.trend, format)
                }
            },
            end = { StreakBadge(s.streak) },
        )
        HeaderLayout.TWO_ROWS -> Column {
            SpaceBetweenRow(start = { TodayCount(s.todayCount, spec.countSp) }, end = { StreakBadge(s.streak) })
            Spacer(modifier = GlanceModifier.height(ROW_GAP_DP.dp))
            TrendRow(s.trend, format)
        }
        HeaderLayout.THREE_ROWS -> Column {
            TodayCount(s.todayCount, spec.countSp)
            Spacer(modifier = GlanceModifier.height(ROW_GAP_DP.dp))
            TrendRow(s.trend, format)
            Spacer(modifier = GlanceModifier.height(ROW_GAP_DP.dp))
            StreakBadge(s.streak)
        }
    }
}

@Composable
private fun HeatmapMode(history: List<DayCommit>, s: CommitSummary, settings: WidgetSettings, m: Metrics) {
    val header = statsHeaderSpec(m, s, settings.trendFormat)
    // Minimalistycznie albo w bardzo niskim widgecie: sama siatka, bez nagłówka.
    val showHeader = !settings.minimal && m.height - header.heightDp - SECTION_GAP_DP >= 24f
    val gridHeight = if (showHeader) m.height - header.heightDp - SECTION_GAP_DP else m.height

    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = if (showHeader) Alignment.Horizontal.Start else Alignment.Horizontal.CenterHorizontally,
    ) {
        if (showHeader) {
            StatsHeader(header, s, settings.trendFormat)
            Spacer(modifier = GlanceModifier.height(SECTION_GAP_DP.dp))
        }
        ContributionGridImage(
            history, s.today, m, m.width, gridHeight,
            maxDays = history.size.coerceAtLeast(7), fitAllDays = false, fixedWeeks = settings.gridWeeks,
        )
    }
}

@Composable
private fun Heatmap30Mode(history: List<DayCommit>, s: CommitSummary, settings: WidgetSettings, m: Metrics) {
    val theme = LocalWidgetTheme.current
    val title = "30 DNI"
    val format = settings.trendFormat
    val oneRow = m.textWidth(title, LABEL_SP) + 8f + trendWidth(m, s.trend, format, withSuffix = false) <= m.width
    val headerHeight = if (oneRow) m.lineHeight(DELTA_SP) else m.lineHeight(LABEL_SP) + ROW_GAP_DP + m.lineHeight(DELTA_SP)
    val showHeader = !settings.minimal && m.height - headerHeight - SECTION_GAP_DP >= 24f
    val gridHeight = if (showHeader) m.height - headerHeight - SECTION_GAP_DP else m.height

    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = if (showHeader) Alignment.Horizontal.Start else Alignment.Horizontal.CenterHorizontally,
    ) {
        if (showHeader) {
            if (oneRow) {
                SpaceBetweenRow(
                    start = { OneLine(title, LABEL_SP, theme.muted) },
                    end = { TrendRow(s.trend, format) },
                )
            } else {
                Column {
                    OneLine(title, LABEL_SP, theme.muted)
                    Spacer(modifier = GlanceModifier.height(ROW_GAP_DP.dp))
                    TrendRow(s.trend, format)
                }
            }
            Spacer(modifier = GlanceModifier.height(SECTION_GAP_DP.dp))
        }
        // Pełna historia jako źródło liczników: siatka domyka pełne tygodnie prawdziwymi danymi,
        // zamiast poszarpanej pierwszej kolumny ucinanej dokładnie na 30. dniu.
        ContributionGridImage(history, s.today, m, m.width, gridHeight, maxDays = 30, fitAllDays = true)
    }
}

@Composable
private fun SparklineMode(rangeHistory: List<DayCommit>, s: CommitSummary, settings: WidgetSettings, m: Metrics) {
    val theme = LocalWidgetTheme.current
    val format = settings.trendFormat
    val countCap = if (m.width >= 220f || m.height >= 140f) 28f else 22f
    val countSp = min(countCap, m.spToFit("${s.todayCount}", m.width * 0.6f))
    val countWidth = m.todayCountWidth(s.todayCount, countSp)
    val oneRow = settings.minimal || countWidth + 8f + trendWidth(m, s.trend, format, withSuffix = false) <= m.width
    val headerHeight = if (oneRow) m.lineHeight(countSp) else m.lineHeight(countSp) + ROW_GAP_DP + m.lineHeight(DELTA_SP)
    // Wykres nie wyższy niż ~0,7 szerokości — w wąskim, wysokim widgecie krzywa rozciągnięta
    // na całą wysokość robi się nieczytelnie szpiczasta.
    val chartHeight = min(m.height - headerHeight - SECTION_GAP_DP, max(m.width * 0.7f, 28f)).coerceAtLeast(12f)
    val bitmap = WidgetGraphics.sparkline(rangeHistory, theme.accent, m.toPx(m.width), m.toPx(chartHeight), m.density)

    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        when {
            settings.minimal -> OneLine("${s.todayCount}", countSp, theme.fg, bold = true)
            oneRow -> SpaceBetweenRow(start = { TodayCount(s.todayCount, countSp) }, end = { TrendRow(s.trend, format) })
            else -> Column {
                TodayCount(s.todayCount, countSp)
                Spacer(modifier = GlanceModifier.height(ROW_GAP_DP.dp))
                TrendRow(s.trend, format)
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
private fun CounterMode(s: CommitSummary, settings: WidgetSettings, m: Metrics) {
    val theme = LocalWidgetTheme.current
    val number = s.todayCount.toString()
    val format = settings.trendFormat

    if (settings.minimal) {
        val sp = min(min(64f, m.spToFit(number, m.width * 0.9f)), m.height / (1.3f * m.fontScale))
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            OneLine(number, sp, theme.fg, bold = true)
        }
        return
    }

    val withSuffix = trendWidth(m, s.trend, format, withSuffix = true) <= m.width
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
                OneLine(number, numberSp, theme.fg, bold = true)
                Spacer(modifier = GlanceModifier.width(6.dp))
                Column {
                    OneLine("commitów", LABEL_SP, theme.muted)
                    OneLine("dzisiaj", LABEL_SP, theme.muted)
                }
            }
        } else {
            OneLine(number, numberSp, theme.fg, bold = true)
            OneLine("commitów", LABEL_SP, theme.muted)
            OneLine("dzisiaj", LABEL_SP, theme.muted)
        }
        Spacer(modifier = GlanceModifier.height(SECTION_GAP_DP.dp))
        TrendRow(s.trend, format, withSuffix = withSuffix)
    }
}

@Composable
private fun GoalMode(s: CommitSummary, settings: WidgetSettings, m: Metrics) {
    val theme = LocalWidgetTheme.current
    val goal = settings.goal
    val format = settings.trendFormat

    val goalText = "cel $goal/dzień".takeIf { m.textWidth(it, 10f) <= m.width } ?: "cel $goal"
    val streakText = "🔥 seria ${s.streak} dni".takeIf { m.textWidth(it, LABEL_SP) <= m.width } ?: "🔥 ${s.streak} dni"
    val infoWidth = maxOf(m.textWidth(goalText, 10f), trendWidth(m, s.trend, format, withSuffix = false), m.textWidth(streakText, LABEL_SP))
    val infoHeight = m.lineHeight(10f) + ROW_GAP_DP + m.lineHeight(DELTA_SP) + ROW_GAP_DP + m.lineHeight(LABEL_SP)

    val rowRing = min(min(m.height, 72f), m.width - 8f - infoWidth)
    val useRow = !settings.minimal && rowRing >= 36f
    val ringDp = when {
        settings.minimal -> min(min(m.width, m.height), 96f)
        useRow -> rowRing
        else -> min(min(m.width, 72f), m.height - infoHeight - 6f).coerceAtLeast(24f)
    }
    val ringBitmap = WidgetGraphics.goalRing(s.todayCount, goal, theme.accent, m.toPx(ringDp))
    val ringNumberSp = min(ringDp * 0.34f / m.fontScale, m.spToFit("${s.todayCount}", ringDp * 0.6f))

    val ring: @Composable () -> Unit = {
        Box(modifier = GlanceModifier.size(ringDp.dp), contentAlignment = Alignment.Center) {
            Image(provider = ImageProvider(ringBitmap), contentDescription = null, modifier = GlanceModifier.size(ringDp.dp))
            OneLine("${s.todayCount}", ringNumberSp, theme.fg, bold = true)
        }
    }
    val info: @Composable () -> Unit = {
        Column(horizontalAlignment = if (useRow) Alignment.Horizontal.Start else Alignment.Horizontal.CenterHorizontally) {
            OneLine(goalText, 10f, theme.fg, bold = true)
            Spacer(modifier = GlanceModifier.height(ROW_GAP_DP.dp))
            TrendRow(s.trend, format)
            Spacer(modifier = GlanceModifier.height(ROW_GAP_DP.dp))
            OneLine(streakText, LABEL_SP, theme.muted)
        }
    }

    when {
        settings.minimal -> Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) { ring() }
        useRow -> Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            ring()
            Spacer(modifier = GlanceModifier.width(8.dp))
            info()
        }
        else -> Column(
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
