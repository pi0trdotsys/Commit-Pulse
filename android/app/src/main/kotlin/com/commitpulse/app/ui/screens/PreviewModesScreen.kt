package com.commitpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.MODES
import com.commitpulse.app.data.WidgetMode
import com.commitpulse.app.data.WidgetSettings
import com.commitpulse.app.ui.components.PhoneFrame
import com.commitpulse.app.ui.components.Section
import com.commitpulse.app.ui.components.WidgetPreview
import com.commitpulse.app.ui.theme.PALETTES

@Composable
fun PreviewModesScreen(
    settings: WidgetSettings,
    history: List<DayCommit>,
    onModeChange: (WidgetMode) -> Unit,
) {
    val pal = PALETTES.getValue(settings.palette)

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        PhoneFrame(modifier = Modifier) {
            WidgetPreview(
                settings = settings,
                history = history,
                pal = pal,
                modifier = Modifier.width(210.dp).height(82.dp),
            )
        }

        Section(title = "Tryb wizualizacji", hint = "Wybierz, żeby zobaczyć podmianę na widgecie powyżej.") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MODES.forEach { m ->
                    val isActive = settings.mode == m.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(16.dp),
                            )
                            .background(if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                            .clickable { onModeChange(m.id) }
                            .padding(14.dp),
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(72.dp, 40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(com.commitpulse.app.ui.theme.WidgetColors.deep)
                                .padding(4.dp),
                        ) {
                            WidgetPreview(settings = settings.copy(mode = m.id), history = history, pal = pal)
                        }
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text(m.nameRes, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(m.descRes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}
