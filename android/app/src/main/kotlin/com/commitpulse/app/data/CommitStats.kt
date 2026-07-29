package com.commitpulse.app.data

import java.time.DayOfWeek
import java.time.LocalDate

/** Liczba commitów (kontrybucji) danego dnia. Ostatni element listy historii = dzisiaj. */
data class DayCommit(val date: LocalDate, val count: Int)

data class WeekDelta(
    val thisWeek: Int,
    val lastWeek: Int,
    val percent: Int,
    val direction: Direction,
    val label: String,
) {
    enum class Direction { UP, DOWN, FLAT }
}

val PL_WEEKDAYS = listOf("Nd", "Pn", "Wt", "Śr", "Cz", "Pt", "So")

fun DayOfWeek.toPlAbbrev(): String = when (this) {
    DayOfWeek.MONDAY -> "Pn"
    DayOfWeek.TUESDAY -> "Wt"
    DayOfWeek.WEDNESDAY -> "Śr"
    DayOfWeek.THURSDAY -> "Cz"
    DayOfWeek.FRIDAY -> "Pt"
    DayOfWeek.SATURDAY -> "So"
    DayOfWeek.SUNDAY -> "Nd"
}

fun List<DayCommit>.lastN(n: Int): List<DayCommit> = if (size <= n) this else subList(size - n, size)

fun List<DayCommit>.today(): Int = lastOrNull()?.count ?: 0

fun List<DayCommit>.streak(): Int {
    var s = 0
    for (i in indices.reversed()) {
        if (this[i].count > 0) s++ else break
    }
    return s
}

fun List<DayCommit>.sum(): Int = sumOf { it.count }

fun List<DayCommit>.maxCount(): Int = maxOfOrNull { it.count } ?: 0

fun List<DayCommit>.weekOverWeek(): WeekDelta {
    val thisWeek = lastN(7).sum()
    val lastWeek = if (size >= 14) subList(size - 14, size - 7).sum() else 0
    val percent = when {
        lastWeek == 0 -> if (thisWeek > 0) 100 else 0
        else -> Math.round(((thisWeek - lastWeek).toDouble() / lastWeek) * 100).toInt()
    }
    val direction = when {
        percent > 1 -> WeekDelta.Direction.UP
        percent < -1 -> WeekDelta.Direction.DOWN
        else -> WeekDelta.Direction.FLAT
    }
    val sign = if (percent > 0) "+" else ""
    return WeekDelta(thisWeek, lastWeek, percent, direction, "$sign$percent%")
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
