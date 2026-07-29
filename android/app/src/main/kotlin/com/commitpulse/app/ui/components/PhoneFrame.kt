package com.commitpulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commitpulse.app.ui.theme.WidgetColors

@Composable
fun PhoneFrame(
    statusLabel: String = "9:41",
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .width(300.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(WidgetColors.deep)
            .padding(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(WidgetColors.bgGradientTop.copy(alpha = 0.55f), Color.Transparent),
                    ),
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(WidgetColors.bgGradientDark1, WidgetColors.bgGradientDark2),
                    ),
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            ) {
                Text(statusLabel, color = WidgetColors.fg.copy(alpha = 0.8f), fontSize = 11.sp)
                Text("▮▮▮ ▲ 84%", color = WidgetColors.fg.copy(alpha = 0.8f), fontSize = 11.sp)
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 24.dp)) {
                content()
            }
        }
    }
}
