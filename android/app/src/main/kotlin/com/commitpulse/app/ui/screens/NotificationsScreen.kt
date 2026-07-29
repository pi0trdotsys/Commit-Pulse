package com.commitpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.WidgetSettings
import com.commitpulse.app.data.lastN
import com.commitpulse.app.data.weekOverWeek
import com.commitpulse.app.ui.components.Chip
import com.commitpulse.app.ui.components.HeatmapStrip
import com.commitpulse.app.ui.components.NotificationMock
import com.commitpulse.app.ui.components.PhoneFrame
import com.commitpulse.app.ui.components.Section
import com.commitpulse.app.ui.theme.PALETTES

private val DAY_LABELS = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")

@Composable
fun NotificationsScreen(
    settings: WidgetSettings,
    history: List<DayCommit>,
    onUpdate: ((WidgetSettings) -> WidgetSettings) -> Unit,
) {
    val pal = PALETTES.getValue(settings.palette)
    val wow = history.weekOverWeek()

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        PhoneFrame(statusLabel = "Pon 9:00") {
            NotificationMock(history = history, pal = pal, modifier = Modifier.width(230.dp))
        }

        Section(title = "Podsumowanie tygodnia", hint = "Wysyłane po zamknięciu minionego tygodnia.") {
            Card {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Włącz cotygodniowe podsumowanie", fontSize = 14.sp)
                    Switch(checked = settings.weeklyDigest, onCheckedChange = { v -> onUpdate { it.copy(weeklyDigest = v) } })
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dzień", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DAY_LABELS.forEachIndexed { i, label ->
                        Chip(settings.digestDay == i + 1, { onUpdate { it.copy(digestDay = i + 1) } }, label)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Godzina", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                OutlinedTextField(
                    value = settings.digestHour,
                    onValueChange = { v -> onUpdate { it.copy(digestHour = v) } },
                    modifier = Modifier.width(120.dp),
                    singleLine = true,
                    placeholder = { Text("09:00") },
                )
            }
        }

        Section(title = "Alerty dodatkowe") {
            Card {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Seria zagrożona", fontSize = 14.sp)
                            Text("Ping o 20:00, jeśli dziś brak commita.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(checked = settings.alertStreak, onCheckedChange = { v -> onUpdate { it.copy(alertStreak = v) } })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Cel dzienny osiągnięty", fontSize = 14.sp)
                            Text("Krótkie potwierdzenie po ${settings.goal} commitach.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(checked = settings.alertGoal, onCheckedChange = { v -> onUpdate { it.copy(alertGoal = v) } })
                    }
                }
            }
        }

        Section(title = "Ostatnie 14 dni", hint = "Dane, na których liczona jest delta tydzień/tydzień.") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(com.commitpulse.app.ui.theme.WidgetColors.deep)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                HeatmapStrip(history.lastN(14), pal.heat, 20.dp, 5.dp, 4.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("Ten tydzień: ${wow.thisWeek}", fontSize = 12.sp, color = com.commitpulse.app.ui.theme.WidgetColors.fg)
                    Text("Poprzedni: ${wow.lastWeek}", fontSize = 12.sp, color = com.commitpulse.app.ui.theme.WidgetColors.fg)
                    Text(wow.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pal.accent)
                }
            }
        }
    }
}
