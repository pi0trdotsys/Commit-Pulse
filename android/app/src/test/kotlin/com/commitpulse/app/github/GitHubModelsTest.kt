package com.commitpulse.app.github

import com.commitpulse.app.data.sum
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Weryfikuje, że mapowanie odpowiedzi GitHub GraphQL na historię commitów liczy je poprawnie:
 * poprawne wartości dzienne, poprawne posortowanie chronologiczne i poprawną sumę.
 */
class GitHubModelsTest {

    private fun week(vararg days: Pair<String, Int>) =
        ContributionWeek(days.map { (date, count) -> ContributionDay(date, count) })

    @Test
    fun `toDayCommits maps every contribution day with its count`() {
        val calendar = ContributionCalendar(
            totalContributions = 6,
            weeks = listOf(
                week("2024-01-01" to 1, "2024-01-02" to 2),
                week("2024-01-03" to 3),
            ),
        )

        val history = calendar.toDayCommits()

        assertEquals(3, history.size)
        assertEquals(LocalDate.of(2024, 1, 1), history[0].date)
        assertEquals(1, history[0].count)
        assertEquals(LocalDate.of(2024, 1, 2), history[1].date)
        assertEquals(2, history[1].count)
        assertEquals(LocalDate.of(2024, 1, 3), history[2].date)
        assertEquals(3, history[2].count)
    }

    @Test
    fun `toDayCommits sums to the same total as the raw contribution days`() {
        val calendar = ContributionCalendar(
            totalContributions = 42,
            weeks = listOf(
                week("2024-02-01" to 5, "2024-02-02" to 0, "2024-02-03" to 10),
                week("2024-02-04" to 4, "2024-02-05" to 23),
            ),
        )
        val rawTotal = calendar.weeks.sumOf { it.contributionDays.sumOf { d -> d.contributionCount } }

        val history = calendar.toDayCommits()

        assertEquals(rawTotal, history.sum())
        assertEquals(42, history.sum())
    }

    @Test
    fun `toDayCommits sorts days chronologically even if weeks arrive out of order`() {
        val calendar = ContributionCalendar(
            totalContributions = 3,
            weeks = listOf(
                week("2024-03-10" to 1),
                week("2024-03-01" to 1),
                week("2024-03-05" to 1),
            ),
        )

        val dates = calendar.toDayCommits().map { it.date }

        assertEquals(
            listOf(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 5), LocalDate.of(2024, 3, 10)),
            dates,
        )
    }

    @Test
    fun `toDayCommits of an empty calendar is empty`() {
        val calendar = ContributionCalendar(totalContributions = 0, weeks = emptyList())

        assertEquals(emptyList<Any>(), calendar.toDayCommits())
    }
}
