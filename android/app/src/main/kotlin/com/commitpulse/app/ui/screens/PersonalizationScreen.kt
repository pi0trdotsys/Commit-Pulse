package com.commitpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commitpulse.app.data.CommitSummary
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.GRID_WEEK_OPTIONS
import com.commitpulse.app.data.Palette
import com.commitpulse.app.data.Range
import com.commitpulse.app.data.RefreshInterval
import com.commitpulse.app.data.Surface as SurfaceOption
import com.commitpulse.app.data.TapAction
import com.commitpulse.app.data.TrendFormat
import com.commitpulse.app.data.WidgetSettings
import com.commitpulse.app.data.hexToRgb
import com.commitpulse.app.data.isValidHexColor
import com.commitpulse.app.data.normalizeHexColor
import com.commitpulse.app.ui.components.Chip
import com.commitpulse.app.ui.components.ChipRow
import com.commitpulse.app.ui.components.Section
import com.commitpulse.app.ui.components.SwitchRow
import com.commitpulse.app.ui.components.WidgetShowcase
import com.commitpulse.app.ui.theme.WidgetColors
import com.commitpulse.app.ui.theme.isMaterialYouSupported
import com.commitpulse.app.ui.theme.paletteFor
import com.commitpulse.app.ui.theme.widgetTheme

@Composable
fun PersonalizationScreen(
    settings: WidgetSettings,
    history: List<DayCommit>,
    summary: CommitSummary,
    onUpdate: ((WidgetSettings) -> WidgetSettings) -> Unit,
) {
    val theme = widgetTheme(LocalContext.current, settings)

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        WidgetShowcase(settings, history)

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SwitchRow(
                    title = "Styl minimalistyczny",
                    subtitle = "Sama siatka, krzywa albo liczba — bez etykiet, trendu i serii.",
                    checked = settings.minimal,
                    onCheckedChange = { v -> onUpdate { it.copy(minimal = v) } },
                )
                SwitchRow(
                    title = "Material You",
                    subtitle = if (isMaterialYouSupported) {
                        "Kolory widgetu i aplikacji dopasowane do tapety."
                    } else {
                        "Wymaga Androida 12 lub nowszego."
                    },
                    checked = settings.materialYou && isMaterialYouSupported,
                    onCheckedChange = { v -> onUpdate { it.copy(materialYou = v) } },
                    enabled = isMaterialYouSupported,
                )
            }
        }

        Section(
            title = "Paleta",
            hint = if (settings.materialYou && isMaterialYouSupported) "Kolory z tapety (Material You). Wyłącz Material You, żeby wybrać własną paletę." else null,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!(settings.materialYou && isMaterialYouSupported)) {
                    ChipRow {
                        Palette.entries.forEach { p ->
                            Chip(
                                active = settings.palette == p,
                                onClick = { onUpdate { it.copy(palette = p) } },
                                label = paletteFor(p, settings.customColorHex).displayName,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    theme.heat.forEach { c ->
                        Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(c))
                    }
                }
            }
        }

        if (!(settings.materialYou && isMaterialYouSupported)) {
            Section(
                title = "Własny kolor (HEX)",
                hint = "Wybierz paletę „Własny” powyżej, żeby użyć go w widgecie.",
            ) {
                HexColorField(settings, onUpdate)
            }
        }

        Section(
            title = "Tygodnie na heatmapie",
            hint = "Mniej tygodni = większe kwadraty. „Auto” mieści tyle tygodni, ile pozwala rozmiar widgetu.",
        ) {
            ChipRow {
                GRID_WEEK_OPTIONS.forEach { weeks ->
                    Chip(
                        active = settings.gridWeeks == weeks,
                        onClick = { onUpdate { it.copy(gridWeeks = weeks) } },
                        label = if (weeks == 0) "Auto" else "$weeks tyg.",
                    )
                }
            }
        }

        Section(
            title = "Wskaźnik trendu",
            hint = "Ostatnie 7 dni vs 7 dni wcześniej (${summary.last7} vs ${summary.previous7}). Szczegóły w zakładce Podgląd.",
        ) {
            ChipRow {
                Chip(
                    settings.trendFormat == TrendFormat.DIFF,
                    { onUpdate { it.copy(trendFormat = TrendFormat.DIFF) } },
                    "Różnica (${summary.trend.diffLabel})",
                )
                Chip(
                    settings.trendFormat == TrendFormat.PERCENT,
                    { onUpdate { it.copy(trendFormat = TrendFormat.PERCENT) } },
                    "Procent (${summary.trend.percentLabel ?: "brak bazy"})",
                )
            }
        }

        Section(title = "Tło widgetu") {
            ChipRow {
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

        Section(title = "Zakres sparkline") {
            ChipRow {
                Range.entries.forEach { r ->
                    Chip(settings.range == r, { onUpdate { it.copy(range = r) } }, "${r.days} dni")
                }
            }
        }

        Section(title = "Odświeżanie") {
            ChipRow {
                Chip(settings.refresh == RefreshInterval.FIFTEEN_MIN, { onUpdate { it.copy(refresh = RefreshInterval.FIFTEEN_MIN) } }, "15 min")
                Chip(settings.refresh == RefreshInterval.ONE_HOUR, { onUpdate { it.copy(refresh = RefreshInterval.ONE_HOUR) } }, "1 godz.")
                Chip(settings.refresh == RefreshInterval.SIX_HOURS, { onUpdate { it.copy(refresh = RefreshInterval.SIX_HOURS) } }, "6 godz.")
            }
        }

        Section(title = "Akcja tapnięcia") {
            ChipRow {
                Chip(settings.tapAction == TapAction.OPEN_APP, { onUpdate { it.copy(tapAction = TapAction.OPEN_APP) } }, "Otwórz aplikację")
                Chip(settings.tapAction == TapAction.OPEN_PROFILE, { onUpdate { it.copy(tapAction = TapAction.OPEN_PROFILE) } }, "Profil GitHub")
                Chip(settings.tapAction == TapAction.REFRESH, { onUpdate { it.copy(tapAction = TapAction.REFRESH) } }, "Odśwież")
            }
        }
    }
}

@Composable
private fun HexColorField(settings: WidgetSettings, onUpdate: ((WidgetSettings) -> WidgetSettings) -> Unit) {
    var hexInput by remember(settings.customColorHex) { mutableStateOf(settings.customColorHex) }
    val valid = isValidHexColor(hexInput)
    val swatch = hexToRgb(hexInput)?.let { Color(it.r, it.g, it.b) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(swatch ?: WidgetColors.muted)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
            )
            OutlinedTextField(
                value = hexInput,
                onValueChange = { new ->
                    hexInput = new
                    normalizeHexColor(new)?.let { normalized ->
                        onUpdate { it.copy(palette = Palette.CUSTOM, customColorHex = normalized) }
                    }
                },
                singleLine = true,
                isError = !valid,
                placeholder = { Text("np. #37D880") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!valid) {
            Text("Nieprawidłowy format — użyj #RGB lub #RRGGBB.", fontSize = 11.sp, color = WidgetColors.trendDown)
        }
    }
}
