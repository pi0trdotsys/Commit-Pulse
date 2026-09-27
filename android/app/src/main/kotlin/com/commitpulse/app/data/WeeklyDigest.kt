package com.commitpulse.app.data

data class WeeklyDigestText(val headline: String, val comparison: String)

/** Treść cotygodniowego podsumowania: miniony tydzień Pn–Nd vs tydzień wcześniej. */
fun weeklyDigestText(week: CalendarWeekSummary): WeeklyDigestText {
    val t = week.trend
    val count = "${t.current} ${plural(t.current, "kontrybucja", "kontrybucje", "kontrybucji")}"
    val comparison = when (t.direction) {
        TrendDirection.FLAT -> "tyle samo co tydzień wcześniej (${t.previous})"
        else -> "${t.diffLabel} vs tydzień wcześniej (${t.previous})"
    }
    return WeeklyDigestText(
        headline = "Miniony tydzień (${formatRange(week.weekStart, week.weekEnd)}): $count",
        comparison = comparison,
    )
}
