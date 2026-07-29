package com.commitpulse.app.ui.components

import androidx.compose.foundation.background
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
import com.commitpulse.app.data.WeekDelta
import com.commitpulse.app.data.lastN
import com.commitpulse.app.data.sum
import com.commitpulse.app.data.weekOverWeek
import com.commitpulse.app.ui.theme.PaletteColors
import com.commitpulse.app.ui.theme.WidgetColors

@Composable
fun NotificationMock(history: List<DayCommit>, pal: PaletteColors, modifier: Modifier = Modifier) {
    val week = history.lastN(7)
    val wow = week.weekOverWeek()
    val up = wow.direction == WeekDelta.Direction.UP

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(WidgetColors.surface.copy(alpha = 0.9f))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(
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
            "Ubiegły tydzień: ${week.sum()} commitów",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = WidgetColors.fg,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(modifier = Modifier.padding(top = 2.dp)) {
            Text(
                (if (up) "▲ " else "▼ ") + wow.label,
                fontSize = 12.sp,
                color = if (up) pal.accent else WidgetColors.trendDown,
            )
            Text(" vs poprzedni tydzień", fontSize = 12.sp, color = WidgetColors.muted)
        }

        HeatmapStrip(week, pal.heat, 16.dp, 4.dp, 3.dp)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            week.forEach { d ->
                Text(
                    d.date.dayOfWeek.toPlAbbrevLocal(),
                    fontSize = 8.sp,
                    color = WidgetColors.muted,
                    modifier = Modifier.size(width = 20.dp, height = 12.dp),
                )
            }
        }
    }
}

private fun java.time.DayOfWeek.toPlAbbrevLocal(): String = PL_WEEKDAYS[
    (this.value % 7),
]
