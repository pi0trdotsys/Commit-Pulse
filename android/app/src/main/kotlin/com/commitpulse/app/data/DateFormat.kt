package com.commitpulse.app.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PL = Locale.forLanguageTag("pl-PL")
private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", PL)
private val DAY = DateTimeFormatter.ofPattern("d", PL)

/** „27 wrz”. */
fun formatDay(date: LocalDate): String = DAY_MONTH.format(date)

/** „21–27 wrz” w obrębie miesiąca, „29 wrz – 5 paź” między miesiącami. */
fun formatRange(from: LocalDate, to: LocalDate): String =
    if (from.year == to.year && from.month == to.month) {
        "${DAY.format(from)}–${DAY_MONTH.format(to)}"
    } else {
        "${DAY_MONTH.format(from)} – ${DAY_MONTH.format(to)}"
    }
