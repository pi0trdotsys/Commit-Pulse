package com.commitpulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.PL_WEEKDAYS
import com.commitpulse.app.data.TrendDirection
import com.commitpulse.app.data.lastCompleteWeek
import com.commitpulse.app.data.weeklyDigestText
import com.commitpulse.app.ui.theme.PaletteColors
import com.commitpulse.app.ui.theme.WidgetColors
import java.time.LocalDate

/** Podgląd cotygodniowego powiadomienia — ta sama treść co prawdziwe podsumowanie. */
@Composable
fun NotificationMock(history: List<DayCommit>, pal: PaletteColors, modifier: Modifier = Modifier) {
    val week = lastCompleteWeek(history, LocalDate.now())
    val digest = weeklyDigestText(week)
    val weekDays = (0L..6L).map { week.weekStart.plusDays(it) }
    val counts = history.associate { it.date to it.count }
    val trendColor = when (week.trend.direction) {
        TrendDirection.UP -> pal.accent
        TrendDirection.DOWN -> WidgetColors.trendDown
        TrendDirection.FLAT -> WidgetColors.muted
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(WidgetColors.surface.copy(alpha = 0.9f))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(pal.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Commit, null, tint = WidgetColors.deep, modifier = Modifier.size(11.dp))
            }
            Text("  Commit Pulse  ·  teraz", fontSize = 11.sp, color = WidgetColors.muted)
        }

        Text(
            digest.headline,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = WidgetColors.fg,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(digest.comparison, fontSize = 12.sp, color = trendColor, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))

        HeatmapStrip(weekDays.map { DayCommit(it, counts[it] ?: 0) }, pal.heat, 16.dp, 4.dp, 3.dp)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            weekDays.forEach { d ->
                Text(
                    PL_WEEKDAYS[d.dayOfWeek.value % 7],
                    fontSize = 8.sp,
                    color = WidgetColors.muted,
                    modifier = Modifier.size(width = 20.dp, height = 12.dp),
                )
            }
        }
    }
}
