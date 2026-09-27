package com.commitpulse.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.roundToInt

/** Liczba kontrybucji GitHub (commity, PR-y, issues, code review) danego dnia. */
data class DayCommit(val date: LocalDate, val count: Int)

val PL_WEEKDAYS = listOf("Nd", "Pn", "Wt", "Śr", "Cz", "Pt", "So")

fun List<DayCommit>.lastN(n: Int): List<DayCommit> = if (size <= n) this else subList(size - n, size)

fun List<DayCommit>.sum(): Int = sumOf { it.count }

fun List<DayCommit>.maxCount(): Int = maxOfOrNull { it.count } ?: 0

fun List<DayCommit>.asDateMap(): Map<LocalDate, Int> = associate { it.date to it.count }

/** Suma kontrybucji w domkniętym zakresie dat (dni bez wpisu liczą się jako 0). */
fun List<DayCommit>.sumBetween(from: LocalDate, to: LocalDate): Int =
    filter { !it.date.isBefore(from) && !it.date.isAfter(to) }.sumOf { it.count }

enum class TrendDirection { UP, DOWN, FLAT }

/**
 * Porównanie dwóch okresów tej samej długości. Procent jest liczony tylko wtedy, gdy poprzedni
 * okres miał jakąkolwiek aktywność — „+100%” przy przejściu z 0 byłoby fałszywe, a przy bardzo
 * małej bazie procent bywa ogromny (6 → 35 = +483%), dlatego domyślnie pokazujemy różnicę.
 */
data class Trend(val current: Int, val previous: Int) {
    val diff: Int get() = current - previous
    val percent: Int? get() = if (previous == 0) null else ((diff.toDouble() / previous) * 100).roundToInt()
    val direction: TrendDirection
        get() = when {
            diff > 0 -> TrendDirection.UP
            diff < 0 -> TrendDirection.DOWN
            else -> TrendDirection.FLAT
        }

    val diffLabel: String get() = if (diff > 0) "+$diff" else if (diff < 0) "−${abs(diff)}" else "0"

    val percentLabel: String?
        get() = percent?.let { if (it > 0) "+$it%" else if (it < 0) "−${abs(it)}%" else "0%" }

    /** Etykieta w wybranym formacie; procent bez bazy (poprzednio 0) wraca do różnicy. */
    fun label(format: TrendFormat): String = when (format) {
        TrendFormat.DIFF -> diffLabel
        TrendFormat.PERCENT -> percentLabel ?: diffLabel
    }
}

/** Wszystkie wskaźniki widgetu liczone w jednym miejscu, względem [today] (dzisiejszej daty urządzenia). */
data class CommitSummary(
    val today: LocalDate,
    val todayCount: Int,
    /** Dni z rzędu z ≥1 kontrybucją; brak kontrybucji dzisiaj nie przerywa serii, dopóki dzień trwa. */
    val streak: Int,
    val streakStart: LocalDate?,
    val last7: Int,
    val previous7: Int,
    val trend: Trend,
) {
    val last7From: LocalDate get() = today.minusDays(6)
    val previous7From: LocalDate get() = today.minusDays(13)
    val previous7To: LocalDate get() = today.minusDays(7)
}

/**
 * Liczy wskaźniki po datach, nie po pozycjach w liście — dzięki temu brakujące dni albo
 * nieaktualna synchronizacja (ostatni wpis sprzed kilku dni) nie przesuwają okien porównania.
 * Okna są kroczące: ostatnie 7 dni (z dzisiejszym) vs 7 dni wcześniej.
 */
fun summarize(history: List<DayCommit>, today: LocalDate): CommitSummary {
    val counts = history.asDateMap()
    val todayCount = counts[today] ?: 0

    var day = if (todayCount > 0) today else today.minusDays(1)
    var streak = 0
    while ((counts[day] ?: 0) > 0) {
        streak++
        day = day.minusDays(1)
    }

    val last7 = history.sumBetween(today.minusDays(6), today)
    val previous7 = history.sumBetween(today.minusDays(13), today.minusDays(7))
    return CommitSummary(
        today = today,
        todayCount = todayCount,
        streak = streak,
        streakStart = if (streak > 0) day.plusDays(1) else null,
        last7 = last7,
        previous7 = previous7,
        trend = Trend(last7, previous7),
    )
}

/** Ostatni pełny tydzień kalendarzowy (Pn–Nd) przed tygodniem zawierającym [today]. */
data class CalendarWeekSummary(val weekStart: LocalDate, val trend: Trend) {
    val weekEnd: LocalDate get() = weekStart.plusDays(6)
}

/** Do cotygodniowego podsumowania: miniony tydzień Pn–Nd vs tydzień przed nim. */
fun lastCompleteWeek(history: List<DayCommit>, today: LocalDate): CalendarWeekSummary {
    val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)
    val current = history.sumBetween(start, start.plusDays(6))
    val previous = history.sumBetween(start.minusWeeks(1), start.minusDays(1))
    return CalendarWeekSummary(start, Trend(current, previous))
}

/** Polska odmiana rzeczownika po liczebniku: 1 kontrybucja, 2 kontrybucje, 5 kontrybucji. */
fun plural(n: Int, one: String, few: String, many: String): String {
    val mod10 = abs(n) % 10
    val mod100 = abs(n) % 100
    return when {
        abs(n) == 1 -> one
        mod10 in 2..4 && mod100 !in 12..14 -> few
        else -> many
    }
}

/** Poziom intensywności 0..4 dla heatmapy. */
fun level(count: Int, max: Int): Int {
    if (count <= 0) return 0
    val q = count.toDouble() / maxOf(max, 1)
    return when {
        q <= 0.25 -> 1
        q <= 0.5 -> 2
        q <= 0.75 -> 3
        else -> 4
    }
}

/**
 * Kalendarzowa siatka kontrybucji w stylu GitHuba: [rows] wierszy dni tygodnia
 * (0 = [firstDayOfWeek] u góry; domyślnie niedziela, jak na github.com) na [columns] kolumn-tygodni,
 * od najstarszej kolumny (lewo) do najnowszej (prawo). Ostatnia kolumna jest wyrównana
 * tak, że "dzisiaj" wypada w prawidłowym wierszu dnia tygodnia; komórki "z przyszłości"
 * w bieżącym tygodniu mają wartość null i nie powinny być rysowane.
 */
fun buildContributionGridDates(
    today: LocalDate,
    columns: Int,
    rows: Int = 7,
    firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
): List<List<LocalDate?>> {
    val todayRow = (today.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    return (0 until rows).map { row ->
        (0 until columns).map { col ->
            val weeksAgo = (columns - 1 - col).toLong()
            val daysAgo = weeksAgo * 7 + (todayRow - row)
            val date = today.minusDays(daysAgo)
            if (date.isAfter(today)) null else date
        }
    }
}
