package com.commitpulse.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Czyste obliczenia układu widgetu (bez Glance/Androida), żeby dało się je przetestować
 * i żeby każdy rozmiar widgetu — od wąskiego 1×2 po szeroki 4×2 — mieścił się bez ucinania.
 */

/** GitHubowe tygodnie-kolumny (szeroki widget) albo kalendarzowe tygodnie-wiersze (wąski, wysoki widget). */
enum class GridOrientation { WEEKS_AS_COLUMNS, WEEKS_AS_ROWS }

data class GridLayout(
    val orientation: GridOrientation,
    val weeks: Int,
    val strideDp: Float,
    val gapRatio: Float,
) {
    val cellDp: Float get() = strideDp * (1f - gapRatio)
    val gapDp: Float get() = strideDp * gapRatio
    val columns: Int get() = if (orientation == GridOrientation.WEEKS_AS_COLUMNS) weeks else 7
    val rows: Int get() = if (orientation == GridOrientation.WEEKS_AS_COLUMNS) 7 else weeks
    val widthDp: Float get() = columns * strideDp - gapDp
    val heightDp: Float get() = rows * strideDp - gapDp
}

private const val DAYS_PER_WEEK = 7

val GridOrientation.firstDayOfWeek: DayOfWeek
    get() = if (this == GridOrientation.WEEKS_AS_COLUMNS) DayOfWeek.SUNDAY else DayOfWeek.MONDAY

/** Ile tygodni-kolumn/wierszy trzeba, żeby pokazać ostatnie [days] dni, gdy bieżący tydzień jest niepełny. */
fun weeksToCover(days: Int, today: LocalDate, firstDayOfWeek: DayOfWeek): Int {
    val daysInCurrentWeek = (today.dayOfWeek.value - firstDayOfWeek.value + 7) % 7 + 1
    val remaining = max(0, days - daysInCurrentWeek)
    return 1 + ceil(remaining / DAYS_PER_WEEK.toDouble()).toInt()
}

private fun layoutFor(
    orientation: GridOrientation,
    longDp: Float,
    shortDp: Float,
    weeksNeeded: Int,
    gapRatio: Float,
    minStrideDp: Float,
    maxStrideDp: Float,
    fitAllDays: Boolean,
): GridLayout {
    val strideMax = min(shortDp / (DAYS_PER_WEEK - gapRatio), maxStrideDp)
    val strideAll = min(strideMax, longDp / (weeksNeeded - gapRatio))
    if (strideAll >= strideMax || (fitAllDays && strideAll >= minStrideDp)) {
        return GridLayout(orientation, weeksNeeded, strideAll, gapRatio)
    }
    if (!fitAllDays) {
        // Komórki tak duże, jak pozwala krótszy bok; tygodni tyle, ile się zmieści.
        val weeks = floor(longDp / strideMax + gapRatio).toInt().coerceIn(1, weeksNeeded)
        return GridLayout(orientation, weeks, min(strideMax, longDp / (weeks - gapRatio)), gapRatio)
    }
    // Nie mieści się całość w czytelnym rozmiarze: tyle tygodni, ile wejdzie przy minimalnej komórce.
    val weeks = floor(longDp / minStrideDp + gapRatio).toInt().coerceIn(1, weeksNeeded)
    return GridLayout(orientation, weeks, min(strideMax, longDp / (weeks - gapRatio)), gapRatio)
}

/**
 * Dobiera orientację i rozmiar komórek siatki kontrybucji tak, żeby zmieściła się w
 * [widthDp]×[heightDp] w całości. Domyślnie układ GitHubowy (tygodnie w kolumnach); układ
 * kalendarzowy (tygodnie w wierszach) wybierany jest tylko wtedy, gdy pokazuje wyraźnie więcej
 * dni — typowo w wąskim, wysokim widgecie 1×2, gdzie mieszczą się ledwie 2–3 kolumny.
 *
 * [fitAllDays] = true: [maxDays] ma się zmieścić w całości (np. tryb 30 dni), nawet kosztem
 * mniejszych komórek (do [minStrideDp]). false: [maxDays] to tylko górny limit dostępnej
 * historii — komórki są tak duże, jak pozwala krótszy bok, a tygodni tyle, ile wejdzie.
 */
fun computeGridLayout(
    widthDp: Float,
    heightDp: Float,
    maxDays: Int,
    today: LocalDate,
    fitAllDays: Boolean = false,
    gapRatio: Float = 0.2f,
    minStrideDp: Float = 5f,
    maxStrideDp: Float = 22f,
): GridLayout {
    val days = maxDays.coerceAtLeast(1)
    fun build(orientation: GridOrientation, longDp: Float, shortDp: Float) = layoutFor(
        orientation, longDp, shortDp, weeksToCover(days, today, orientation.firstDayOfWeek),
        gapRatio, minStrideDp, maxStrideDp, fitAllDays,
    )
    val columns = build(GridOrientation.WEEKS_AS_COLUMNS, widthDp, heightDp)
    val rows = build(GridOrientation.WEEKS_AS_ROWS, heightDp, widthDp)

    val daysColumns = min(columns.weeks * DAYS_PER_WEEK, days)
    val daysRows = min(rows.weeks * DAYS_PER_WEEK, days)
    return when {
        daysRows > daysColumns * 1.5f -> rows
        daysRows == daysColumns && rows.strideDp > columns.strideDp * 1.2f -> rows
        else -> columns
    }
}

/**
 * Komórki siatki w kolejności wyświetlania (wiersz po wierszu); null = dzień z przyszłości
 * albo sprzed [earliest] (poza zakresem danych — nie rysujemy go jako „0 commitów”).
 * Układ GitHubowy zaczyna tydzień od niedzieli (jak github.com), kalendarzowy — od poniedziałku,
 * jak polski kalendarz, żeby wiersze czytały się jak tygodnie Pn–Nd.
 */
fun buildGridCells(today: LocalDate, layout: GridLayout, earliest: LocalDate? = null): List<List<LocalDate?>> {
    val weekColumns = buildContributionGridDates(today, layout.weeks, DAYS_PER_WEEK, layout.orientation.firstDayOfWeek)
        .map { row -> row.map { date -> date?.takeUnless { earliest != null && it.isBefore(earliest) } } }
    return when (layout.orientation) {
        GridOrientation.WEEKS_AS_COLUMNS -> weekColumns
        GridOrientation.WEEKS_AS_ROWS -> (0 until layout.weeks).map { week -> weekColumns.map { it[week] } }
    }
}

/** Przybliżona szerokość tekstu w dp (średnia szerokość znaku Roboto ≈ 0,56 em, emoji ≈ 1,15 em). */
fun estimateTextWidthDp(text: String, sizeSp: Float, fontScale: Float = 1f): Float {
    val units = text.codePoints().toArray().sumOf { cp -> if (cp > 0x2000) 1.15 else 0.56 }
    return (units * sizeSp * fontScale).toFloat()
}

/** Wysokość jednej linii tekstu w dp. */
fun lineHeightDp(sizeSp: Float, fontScale: Float = 1f): Float = sizeSp * fontScale * 1.3f

/** Zwarta etykieta zmiany tydzień/tydzień: kierunek pokazuje strzałka, więc bez znaku "+". */
fun WeekDelta.compactLabel(): String = "${kotlin.math.abs(percent)}%"

enum class HeaderLayout { ONE_ROW, TWO_ROWS, THREE_ROWS }

/**
 * Wybiera najbardziej zwarty układ nagłówka (licznik dnia, zmiana w/w, seria), który mieści
 * się w [widthDp] bez zawijania ani ucinania tekstu.
 */
fun chooseHeaderLayout(
    widthDp: Float,
    countWidthDp: Float,
    deltaWidthDp: Float,
    streakWidthDp: Float,
    spacingDp: Float = 8f,
): HeaderLayout = when {
    countWidthDp + deltaWidthDp + streakWidthDp + spacingDp * 2 <= widthDp -> HeaderLayout.ONE_ROW
    countWidthDp + streakWidthDp + spacingDp <= widthDp && deltaWidthDp <= widthDp -> HeaderLayout.TWO_ROWS
    else -> HeaderLayout.THREE_ROWS
}
