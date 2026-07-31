package com.commitpulse.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.Surface
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
import com.commitpulse.app.ui.theme.PaletteColors
import com.commitpulse.app.ui.theme.WidgetColors
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun HeatmapStrip(
    days: List<DayCommit>,
    heat: List<Color>,
    size: Dp = 12.dp,
    gap: Dp = 3.dp,
    radius: Dp = 2.dp,
) {
    val maxVal = days.maxCount()
    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Bottom) {
        days.forEach { d ->
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(radius))
                    .background(heat[level(d.count, maxVal)]),
            )
        }
    }
}

@Composable
fun Sparkline(days: List<DayCommit>, accent: Color, width: Dp = 150.dp, height: Dp = 26.dp) {
    Canvas(modifier = Modifier.width(width).height(height)) {
        if (days.isEmpty()) return@Canvas
        val maxVal = max(days.maxOf { it.count }, 1)
        val w = size.width
        val h = size.height
        val step = w / max(days.size - 1, 1)
        val points = days.mapIndexed { i, d ->
            Offset(i * step, h - (d.count.toFloat() / maxVal) * (h - 3f) - 1.5f)
        }
        val line = Path().apply {
            points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(area, color = accent.copy(alpha = 0.16f))
        drawPath(
            line,
            color = accent,
            style = Stroke(width = 1.6f * density, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawCircle(color = accent, radius = 2.2f * density, center = points.last())
    }
}

@Composable
fun GoalRing(value: Int, goal: Int, accent: Color, size: Dp = 46.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val strokeWidth = this.size.width * 0.09f
        val radius = this.size.width / 2f - strokeWidth
        val pct = (value.toFloat() / max(goal, 1)).coerceIn(0f, 1f)
        val topLeft = Offset(this.size.width / 2f - radius, this.size.height / 2f - radius)
        val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        drawArc(
            color = Color(0xFF52586A),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
            color = accent,
            startAngle = -90f,
            sweepAngle = 360f * pct,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun Delta(accent: Color, wow: WeekDelta, compact: Boolean = false) {
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(arrow, fontSize = 9.sp, color = color)
        Text(" ${wow.label}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
        if (!compact) {
            Text(" vs poprz. 7 dni", fontSize = 10.sp, color = WidgetColors.muted)
        }
    }
}

/**
 * Prawdziwa kalendarzowa siatka kontrybucji (7 wierszy dni tygodnia × N kolumn-tygodni),
 * jak na github.com. Rozmiar komórki dopasowuje się do miejsca dostępnego w rodzicu
 * ([BoxWithConstraints]) — ten sam algorytm co w prawdziwym widgecie (Glance), żeby
 * podgląd w aplikacji wiarygodnie odzwierciedlał to, co wyląduje na ekranie głównym.
 */
@Composable
fun ContributionGridCompose(
    history: List<DayCommit>,
    heat: List<Color>,
    modifier: Modifier = Modifier,
    maxDays: Int = Int.MAX_VALUE,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val today = history.lastOrNull()?.date ?: LocalDate.now()
        val countMap = history.asDateMap()
        val maxVal = history.maxCount()

        val minStride = 5.dp
        val rows = (maxHeight / minStride).toInt().coerceIn(1, 7)
        val strideFromHeight = maxHeight / rows

        val gapRatio = 0.22f
        val rawCell = strideFromHeight * (1f - gapRatio)
        val cell = if (rawCell < 2.dp) 2.dp else rawCell
        val gap = if (rawCell < 2.dp) 1.dp else strideFromHeight * gapRatio
        val stride = cell + gap

        val columnsFit = (maxWidth / stride).toInt()
        val maxColumnsForDays = ceil(maxDays.toDouble() / rows).toInt().coerceAtLeast(4)
        val columns = columnsFit.coerceIn(4, maxColumnsForDays)

        val grid = buildContributionGridDates(today, columns, rows)

        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            grid.forEach { rowDates ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    rowDates.forEach { date ->
                        if (date == null) {
                            Box(modifier = Modifier.size(cell))
                        } else {
                            val count = countMap[date] ?: 0
                            Box(
                                modifier = Modifier
                                    .size(cell)
                                    .clip(RoundedCornerShape(cell / 4))
                                    .background(heat[level(count, maxVal)]),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Widget 2×1 renderowany w skali — odpowiednik WidgetPreview.tsx. */
@Composable
fun WidgetPreview(
    settings: WidgetSettings,
    history: List<DayCommit>,
    pal: PaletteColors,
    modifier: Modifier = Modifier,
) {
    val bg = when (settings.surface) {
        Surface.TRANSPARENT -> Color.Transparent
        Surface.CARD -> WidgetColors.surface.copy(alpha = 0.9f)
        Surface.DARK -> WidgetColors.deep
    }
    val today = history.today()
    val st = history.streak()
    val wow = history.weekOverWeek()
    val rangeDays = if (settings.mode == WidgetMode.HEATMAP_30) 30 else settings.range.days

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(12.dp),
    ) {
        when (settings.mode) {
            WidgetMode.HEATMAP -> Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(today.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WidgetColors.fg)
                        Text(" dziś", fontSize = 8.sp, color = WidgetColors.muted)
                        Spacer(Modifier.width(7.dp))
                        Delta(pal.accent, wow, compact = true)
                    }
                    StreakBadge(st, pal.accent)
                }
                Spacer(Modifier.height(5.dp))
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    ContributionGridCompose(history, pal.heat)
                }
            }

            WidgetMode.SPARKLINE -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(today.toString(), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = WidgetColors.fg)
                        Text(" dziś", fontSize = 9.sp, color = WidgetColors.muted)
                    }
                    Delta(pal.accent, wow, compact = true)
                }
                Sparkline(history.lastN(14), pal.accent, 156.dp, 24.dp)
            }

            WidgetMode.COUNTER -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(today.toString(), fontSize = 44.sp, fontWeight = FontWeight.Bold, color = WidgetColors.fg)
                    Column {
                        Text("commitów", fontSize = 9.sp, color = WidgetColors.muted)
                        Text("dzisiaj", fontSize = 9.sp, color = WidgetColors.muted)
                    }
                }
                Delta(pal.accent, wow)
            }

            WidgetMode.GOAL -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    GoalRing(today, settings.goal, pal.accent, 46.dp)
                    Text(today.toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WidgetColors.fg)
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("cel ${settings.goal}/dzień", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = WidgetColors.fg)
                    Delta(pal.accent, wow)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = WidgetColors.muted, modifier = Modifier.size(10.dp))
                        Text(" seria $st dni", fontSize = 9.sp, color = WidgetColors.muted)
                    }
                }
            }

            WidgetMode.HEATMAP_30 -> Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("30 DNI", fontSize = 9.sp, color = WidgetColors.muted)
                    Delta(pal.accent, history.lastN(rangeDays).weekOverWeek(), compact = true)
                }
                Spacer(Modifier.height(5.dp))
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    ContributionGridCompose(history.lastN(rangeDays), pal.heat, maxDays = 30)
                }
            }
        }
    }
}

@Composable
private fun StreakBadge(streak: Int, accent: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.LocalFireDepartment, null, tint = accent, modifier = Modifier.size(10.dp))
        Text(" ${streak}d", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accent)
    }
}
