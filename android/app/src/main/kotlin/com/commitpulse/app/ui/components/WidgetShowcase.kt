package com.commitpulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.WidgetSettings
import com.commitpulse.app.ui.theme.WidgetColors

/** Wymiary typowego widgetu na ekranie głównym (zmierzone na telefonie z siatką 5 kolumn). */
val WIDGET_1X2 = 61.dp to 172.dp
val WIDGET_2X1 = 150.dp to 80.dp

private val wallpaper = Brush.linearGradient(listOf(WidgetColors.bgGradientTop, WidgetColors.bgGradientDark2))

/** Widget na żywo w dwóch rozmiarach, na tle imitującym tapetę. */
@Composable
fun WidgetShowcase(settings: WidgetSettings, history: List<DayCommit>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(wallpaper)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
            Captioned("1×2") { LiveWidgetPreview(settings, history, WIDGET_1X2.first, WIDGET_1X2.second) }
            Captioned("2×1") { LiveWidgetPreview(settings, history, WIDGET_2X1.first, WIDGET_2X1.second) }
        }
    }
}

@Composable
private fun Captioned(caption: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        content()
        Text(caption, fontSize = 11.sp, color = WidgetColors.fg.copy(alpha = 0.7f))
    }
}

/** Pomniejszona miniatura widgetu 2×1 (renderowana w pełnym rozmiarze i skalowana, żeby układ był prawdziwy). */
@Composable
fun WidgetThumbnail(settings: WidgetSettings, history: List<DayCommit>, scale: Float = 0.5f) {
    val (w, h) = WIDGET_2X1
    Box(
        modifier = Modifier
            .size(w * scale, h * scale)
            .clip(RoundedCornerShape(10.dp))
            .background(wallpaper),
        contentAlignment = Alignment.Center,
    ) {
        LiveWidgetPreview(
            settings, history, w, h,
            modifier = Modifier.requiredSize(w, h).graphicsLayer { scaleX = scale; scaleY = scale },
        )
    }
}
