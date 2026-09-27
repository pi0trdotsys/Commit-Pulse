package com.commitpulse.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commitpulse.app.data.CommitSummary
import com.commitpulse.app.data.formatDay
import com.commitpulse.app.data.formatRange
import com.commitpulse.app.data.plural
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun contributions(n: Int) = "$n ${plural(n, "kontrybucja", "kontrybucje", "kontrybucji")}"

private val SYNC_TIME = DateTimeFormatter.ofPattern("HH:mm")

/** Rozbicie wskaźników widgetu na konkretne liczby i daty + wyjaśnienie, jak są liczone. */
@Composable
fun StatsExplanation(summary: CommitSummary, lastSyncEpochMs: Long?) {
    val s = summary
    val trend = s.trend
    val trendValue = trend.percentLabel?.let { "${trend.diffLabel} (${it})" } ?: trend.diffLabel

    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StatLine("Dziś", formatDay(s.today), contributions(s.todayCount))
            StatLine(
                "Seria",
                s.streakStart?.let { "od ${formatDay(it)}" } ?: "brak aktywnej serii",
                "${s.streak} ${plural(s.streak, "dzień", "dni", "dni")}",
            )
            StatLine("Ostatnie 7 dni", formatRange(s.last7From, s.today), contributions(s.last7))
            StatLine("Poprzednie 7 dni", formatRange(s.previous7From, s.previous7To), contributions(s.previous7))
            StatLine("Trend", "ostatnie 7 vs poprzednie 7", trendValue)

            HorizontalDivider()

            Explanation(
                "Trend",
                "Suma z ostatnich 7 dni (razem z dzisiejszym) minus suma z 7 dni wcześniej. " +
                    "Okno przesuwa się codziennie, więc nie zeruje się w poniedziałek rano.",
            )
            Explanation(
                "Różnica czy procent?",
                buildString {
                    append("Różnica mówi wprost, o ile kontrybucji więcej lub mniej niż tydzień wcześniej. ")
                    append("Procent przy małej bazie bywa ogromny")
                    if (trend.percent != null && s.previous7 in 1..9) {
                        append(" — u Ciebie ${s.previous7} → ${s.last7} to ${trend.percentLabel}")
                    }
                    append(", dlatego domyślnie widget pokazuje różnicę. ")
                    append("Gdy w poprzednich 7 dniach nie było nic, procentu nie da się policzyć — wtedy zawsze widać różnicę.")
                },
            )
            Explanation(
                "Seria",
                "Liczba dni z rzędu z co najmniej jedną kontrybucją. Dopóki dzisiejszy dzień trwa, " +
                    "brak kontrybucji dziś nie przerywa serii — liczy się od wczoraj.",
            )
            Explanation(
                "Źródło danych",
                "Kalendarz kontrybucji z Twojego profilu GitHub — ta sama zielona siatka: commity, pull requesty, " +
                    "issues i code review. Prywatne repozytoria liczą się tylko przy włączonej opcji " +
                    "„Private contributions” w ustawieniach profilu. Kolor kwadratu to intensywność względem " +
                    "najaktywniejszego dnia widocznego na siatce.",
            )
            Text(
                lastSyncEpochMs?.let {
                    val time = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                    "Ostatnia synchronizacja: ${formatDay(time.toLocalDate())}, ${SYNC_TIME.format(time)}"
                } ?: "Jeszcze nie zsynchronizowano z GitHubem.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun StatLine(label: String, detail: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Explanation(title: String, body: String) {
    Column {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}
