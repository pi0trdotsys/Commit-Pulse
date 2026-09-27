package com.commitpulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.level
import com.commitpulse.app.data.maxCount

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
