package com.commitpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commitpulse.app.data.CommitSummary
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.MODES
import com.commitpulse.app.data.WidgetMode
import com.commitpulse.app.data.WidgetSettings
import com.commitpulse.app.ui.components.Section
import com.commitpulse.app.ui.components.StatsExplanation
import com.commitpulse.app.ui.components.WidgetShowcase
import com.commitpulse.app.ui.components.WidgetThumbnail

@Composable
fun PreviewModesScreen(
    settings: WidgetSettings,
    history: List<DayCommit>,
    summary: CommitSummary,
    lastSyncEpochMs: Long?,
    onModeChange: (WidgetMode) -> Unit,
) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        WidgetShowcase(settings, history)

        Section(title = "Tryb wizualizacji", hint = "Wybierz, żeby zobaczyć podmianę na widgecie powyżej i na ekranie głównym.") {
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
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WidgetThumbnail(settings.copy(mode = m.id), history)
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text(m.nameRes, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(m.descRes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }

        Section(title = "Skąd te liczby?", hint = "Wszystkie wskaźniki z widgetu, rozpisane na konkretne dni.") {
            StatsExplanation(summary, lastSyncEpochMs)
        }
    }
}
