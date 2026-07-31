package com.commitpulse.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
import com.commitpulse.app.data.Surface as WidgetSurface
import com.commitpulse.app.data.TapAction
import com.commitpulse.app.data.WeekDelta
import com.commitpulse.app.data.WidgetMode
import com.commitpulse.app.data.WidgetSettings
import com.commitpulse.app.data.asDateMap
import com.commitpulse.app.data.buildContributionGridDates
import com.commitpulse.app.data.lastN
import com.commitpulse.app.data.level
import com.commitpulse.app.data.maxCount
import com.commitpulse.app.data.streak
import com.commitpulse.app.data.today
import com.commitpulse.app.data.weekOverWeek
import com.commitpulse.app.github.GitHubAccount
import com.commitpulse.app.ui.MainActivity
import com.commitpulse.app.ui.theme.PALETTES
import com.commitpulse.app.ui.theme.PaletteColors
import com.commitpulse.app.ui.theme.WidgetColors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import kotlin.math.ceil

class CommitPulseWidget : GlanceAppWidget() {

    /** Rozmiar rzeczywisty widgetu (nie zadeklarowane minimum) — potrzebny do responsywnej siatki heatmapy. */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.commitPulseApp()
        val settings = app.settingsRepository.currentSettings()
        val history = app.settingsRepository.historyFlow.first()
        val account = app.settingsRepository.accountFlow.first()
        val pal = PALETTES.getValue(settings.palette)

        val rangeDays = if (settings.mode == WidgetMode.HEATMAP_30) 30 else settings.range.days
        val rangeHistory = history.lastN(rangeDays)

        val density = context.resources.displayMetrics.density
        val sparklineBitmap = if (settings.mode == WidgetMode.SPARKLINE) {
            WidgetGraphics.sparkline(rangeHistory, pal.accent, (156 * density).toInt(), (24 * density).toInt())
        } else null
        val goalRingBitmap = if (settings.mode == WidgetMode.GOAL) {
            WidgetGraphics.goalRing(history.today(), settings.goal, pal.accent, (46 * density).toInt())
        } else null

        provideContent {
            CommitPulseWidgetContent(
                settings = settings,
                pal = pal,
                history = history,
                rangeHistory = rangeHistory,
                account = account,
                sparklineBitmap = sparklineBitmap,
                goalRingBitmap = goalRingBitmap,
            )
        }
    }
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
    account: GitHubAccount?,
    sparklineBitmap: android.graphics.Bitmap?,
    goalRingBitmap: android.graphics.Bitmap?,
) {
    val bg = when (settings.surface) {
        WidgetSurface.TRANSPARENT -> ColorProvider(Color.Transparent)
        WidgetSurface.CARD -> ColorProvider(WidgetColors.surface.copy(alpha = 0.92f))
        WidgetSurface.DARK -> ColorProvider(WidgetColors.deep)
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bg)
            .cornerRadius(14.dp)
            .padding(10.dp)
            .clickable(tapAction(settings.tapAction)),
    ) {
        when (settings.mode) {
            WidgetMode.HEATMAP -> HeatmapMode(history, pal)
            WidgetMode.SPARKLINE -> SparklineMode(history, sparklineBitmap)
            WidgetMode.COUNTER -> CounterMode(history, pal)
            WidgetMode.GOAL -> GoalMode(history, settings.goal, pal, goalRingBitmap)
            WidgetMode.HEATMAP_30 -> Heatmap30Mode(rangeHistory, pal)
        }
    }
}

/** Dwie w pełni szerokie warstwy Boxa wyrównane do lewej/prawej — zastępstwo braku Arrangement.SpaceBetween w Glance. */
@Composable
private fun SpaceBetweenRow(start: @Composable () -> Unit, end: @Composable () -> Unit) {
    Box(modifier = GlanceModifier.fillMaxWidth()) {
        Box(modifier = GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) { start() }
        Box(modifier = GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { end() }
    }
}

@Composable
private fun TodayCount(today: Int, fontSize: TextUnit) {
    Row(verticalAlignment = Alignment.Vertical.Bottom) {
        Text(
            text = today.toString(),
            style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Bold, color = ColorProvider(WidgetColors.fg)),
        )
        Spacer(modifier = GlanceModifier.width(5.dp))
        Text(
            text = "dziś",
            style = TextStyle(fontSize = 8.sp, color = ColorProvider(WidgetColors.muted)),
        )
    }
}

@Composable
private fun StreakBadge(streak: Int, accent: Color) {
    Row(
        modifier = GlanceModifier
            .background(ColorProvider(accent.copy(alpha = 0.18f)))
            .cornerRadius(20.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text("🔥", style = TextStyle(fontSize = 8.sp))
        Spacer(modifier = GlanceModifier.width(2.dp))
        Text("${streak}d", style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ColorProvider(accent)))
    }
}

@Composable
private fun DeltaRow(wow: WeekDelta, accent: Color, compact: Boolean = false) {
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
        Text(arrow, style = TextStyle(fontSize = 8.sp, color = ColorProvider(color)))
        Spacer(modifier = GlanceModifier.width(3.dp))
        Text(wow.label, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ColorProvider(color)))
        if (!compact) {
            Spacer(modifier = GlanceModifier.width(3.dp))
            Text("vs poprz. 7 dni", style = TextStyle(fontSize = 8.sp, color = ColorProvider(WidgetColors.muted)))
        }
    }
}

/**
 * Prawdziwa kalendarzowa siatka kontrybucji (7 wierszy dni tygodnia × N kolumn-tygodni),
 * jak na github.com — nie jednorzędowy pasek. Rozmiar komórki wyliczany responsywnie
 * z [availableWidth]/[availableHeight] (rzeczywisty rozmiar widgetu, patrz [SizeMode.Exact]),
 * więc po powiększeniu widgetu na ekranie głównym siatka pokazuje więcej tygodni, a nie
 * tylko większe te same 14 dni.
 */
@Composable
private fun ContributionGrid(
    history: List<DayCommit>,
    heat: List<Color>,
    availableWidth: Dp,
    availableHeight: Dp,
    maxDays: Int = Int.MAX_VALUE,
) {
    val today = history.lastOrNull()?.date ?: LocalDate.now()
    val countMap = history.asDateMap()
    val maxVal = history.maxCount()

    val minStride = 5.dp
    val rows = (availableHeight / minStride).toInt().coerceIn(1, 7)
    val strideFromHeight = availableHeight / rows

    val gapRatio = 0.22f
    val rawCell = strideFromHeight * (1f - gapRatio)
    val cell = if (rawCell < 2.dp) 2.dp else rawCell
    val gap = if (rawCell < 2.dp) 1.dp else strideFromHeight * gapRatio
    val stride = cell + gap

    val columnsFit = (availableWidth / stride).toInt()
    val maxColumnsForDays = ceil(maxDays.toDouble() / rows).toInt().coerceAtLeast(4)
    val columns = columnsFit.coerceIn(4, maxColumnsForDays)

    val grid = buildContributionGridDates(today, columns, rows)

    Column {
        grid.forEachIndexed { rowIndex, rowDates ->
            if (rowIndex > 0) Spacer(modifier = GlanceModifier.height(gap))
            Row {
                rowDates.forEachIndexed { colIndex, date ->
                    if (colIndex > 0) Spacer(modifier = GlanceModifier.width(gap))
                    if (date == null) {
                        Spacer(modifier = GlanceModifier.size(cell))
                    } else {
                        val count = countMap[date] ?: 0
                        Box(
                            modifier = GlanceModifier
                                .size(cell)
                                .background(ColorProvider(heat[level(count, maxVal)]))
                                .cornerRadius(cell / 4),
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapMode(history: List<DayCommit>, pal: PaletteColors) {
    val size = LocalSize.current
    val outerPadding = 10.dp
    val headerHeight = 18.dp
    val headerGap = 5.dp
    val gridWidth = (size.width - outerPadding * 2).let { if (it < 40.dp) 40.dp else it }
    val gridHeight = (size.height - outerPadding * 2 - headerHeight - headerGap).let { if (it < 16.dp) 16.dp else it }

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Box(modifier = GlanceModifier.fillMaxWidth().height(headerHeight)) {
            SpaceBetweenRow(
                start = {
                    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                        TodayCount(history.today(), 15.sp)
                        Spacer(modifier = GlanceModifier.width(7.dp))
                        DeltaRow(history.weekOverWeek(), pal.accent, compact = true)
                    }
                },
                end = { StreakBadge(history.streak(), pal.accent) },
            )
        }
        Spacer(modifier = GlanceModifier.height(headerGap))
        ContributionGrid(history, pal.heat, gridWidth, gridHeight)
    }
}

@Composable
private fun SparklineMode(history: List<DayCommit>, bitmap: android.graphics.Bitmap?) {
    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        SpaceBetweenRow(
            start = { TodayCount(history.today(), 22.sp) },
            end = { DeltaRow(history.weekOverWeek(), WidgetColors.trendUp, compact = true) },
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxWidth().height(24.dp),
            )
        }
    }
}

@Composable
private fun CounterMode(history: List<DayCommit>, pal: PaletteColors) {
    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        Row(verticalAlignment = Alignment.Vertical.Bottom) {
            Text(
                history.today().toString(),
                style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WidgetColors.fg)),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Column {
                Text("commitów", style = TextStyle(fontSize = 8.sp, color = ColorProvider(WidgetColors.muted)))
                Text("dzisiaj", style = TextStyle(fontSize = 8.sp, color = ColorProvider(WidgetColors.muted)))
            }
        }
        Spacer(modifier = GlanceModifier.height(3.dp))
        DeltaRow(history.weekOverWeek(), pal.accent)
    }
}

@Composable
private fun GoalMode(history: List<DayCommit>, goal: Int, pal: PaletteColors, ringBitmap: android.graphics.Bitmap?) {
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        Box(modifier = GlanceModifier.size(46.dp), contentAlignment = Alignment.Center) {
            if (ringBitmap != null) {
                Image(provider = ImageProvider(ringBitmap), contentDescription = null, modifier = GlanceModifier.size(46.dp))
            }
            Text(history.today().toString(), style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WidgetColors.fg)))
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column {
            Text("cel $goal/dzień", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WidgetColors.fg)))
            Spacer(modifier = GlanceModifier.height(2.dp))
            DeltaRow(history.weekOverWeek(), pal.accent)
            Spacer(modifier = GlanceModifier.height(2.dp))
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text("🔥", style = TextStyle(fontSize = 8.sp))
                Spacer(modifier = GlanceModifier.width(2.dp))
                Text("seria ${history.streak()} dni", style = TextStyle(fontSize = 8.sp, color = ColorProvider(WidgetColors.muted)))
            }
        }
    }
}

@Composable
private fun Heatmap30Mode(rangeHistory: List<DayCommit>, pal: PaletteColors) {
    val size = LocalSize.current
    val outerPadding = 10.dp
    val headerHeight = 14.dp
    val headerGap = 5.dp
    val gridWidth = (size.width - outerPadding * 2).let { if (it < 40.dp) 40.dp else it }
    val gridHeight = (size.height - outerPadding * 2 - headerHeight - headerGap).let { if (it < 16.dp) 16.dp else it }

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Box(modifier = GlanceModifier.fillMaxWidth().height(headerHeight)) {
            SpaceBetweenRow(
                start = { Text("30 DNI", style = TextStyle(fontSize = 8.sp, color = ColorProvider(WidgetColors.muted))) },
                end = { DeltaRow(rangeHistory.weekOverWeek(), pal.accent, compact = true) },
            )
        }
        Spacer(modifier = GlanceModifier.height(headerGap))
        ContributionGrid(rangeHistory, pal.heat, gridWidth, gridHeight, maxDays = 30)
    }
}
