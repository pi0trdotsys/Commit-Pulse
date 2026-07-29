package com.commitpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.commitpulse.app.data.Palette
import com.commitpulse.app.data.Range
import com.commitpulse.app.data.RefreshInterval
import com.commitpulse.app.data.Surface as SurfaceOption
import com.commitpulse.app.data.TapAction
import com.commitpulse.app.data.WidgetSettings
import com.commitpulse.app.ui.components.Chip
import com.commitpulse.app.ui.components.Section
import com.commitpulse.app.ui.components.WidgetPreview
import com.commitpulse.app.ui.theme.PALETTES

@Composable
fun PersonalizationScreen(
    settings: WidgetSettings,
    history: List<com.commitpulse.app.data.DayCommit>,
    onUpdate: ((WidgetSettings) -> WidgetSettings) -> Unit,
) {
    val pal = PALETTES.getValue(settings.palette)

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(com.commitpulse.app.ui.theme.WidgetColors.deep)
                .padding(16.dp),
        ) {
            WidgetPreview(settings = settings, history = history, pal = pal, modifier = Modifier.size(220.dp, 70.dp))
        }

        Section(title = "Paleta") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Palette.entries.forEach { p ->
                        Chip(active = settings.palette == p, onClick = { onUpdate { it.copy(palette = p) } }, label = PALETTES.getValue(p).displayName)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pal.heat.forEach { c ->
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(c),
                        )
                    }
                }
            }
        }

        Section(title = "Tło widgetu") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(settings.surface == SurfaceOption.TRANSPARENT, { onUpdate { it.copy(surface = SurfaceOption.TRANSPARENT) } }, "Przezroczyste")
                Chip(settings.surface == SurfaceOption.CARD, { onUpdate { it.copy(surface = SurfaceOption.CARD) } }, "Karta")
                Chip(settings.surface == SurfaceOption.DARK, { onUpdate { it.copy(surface = SurfaceOption.DARK) } }, "Ciemna karta")
            }
        }

        Section(title = "Cel dzienny — ${settings.goal} commitów") {
            Slider(
                value = settings.goal.toFloat(),
                onValueChange = { onUpdate { s -> s.copy(goal = it.toInt().coerceIn(1, 20)) } },
                valueRange = 1f..20f,
                steps = 18,
            )
        }

        Section(title = "Zakres dni") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Range.entries.forEach { r ->
                    Chip(settings.range == r, { onUpdate { it.copy(range = r) } }, "${r.days} dni")
                }
            }
        }

        Section(title = "Odświeżanie") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(settings.refresh == RefreshInterval.FIFTEEN_MIN, { onUpdate { it.copy(refresh = RefreshInterval.FIFTEEN_MIN) } }, "15 min")
                Chip(settings.refresh == RefreshInterval.ONE_HOUR, { onUpdate { it.copy(refresh = RefreshInterval.ONE_HOUR) } }, "1 godz.")
                Chip(settings.refresh == RefreshInterval.SIX_HOURS, { onUpdate { it.copy(refresh = RefreshInterval.SIX_HOURS) } }, "6 godz.")
            }
        }

        Section(title = "Akcja tapnięcia") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(settings.tapAction == TapAction.OPEN_APP, { onUpdate { it.copy(tapAction = TapAction.OPEN_APP) } }, "Otwórz aplikację")
                Chip(settings.tapAction == TapAction.OPEN_PROFILE, { onUpdate { it.copy(tapAction = TapAction.OPEN_PROFILE) } }, "Profil GitHub")
                Chip(settings.tapAction == TapAction.REFRESH, { onUpdate { it.copy(tapAction = TapAction.REFRESH) } }, "Odśwież")
            }
        }
    }
}
