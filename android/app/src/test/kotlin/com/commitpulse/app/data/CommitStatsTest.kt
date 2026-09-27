package com.commitpulse.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val BASE_DATE: LocalDate = LocalDate.of(2024, 1, 1)

/** Buduje historię z listy dziennych liczników, zaczynając [BASE_DATE] dnia i idąc kolejno naprzód. */
private fun history(vararg counts: Int): List<DayCommit> =
    counts.mapIndexed { i, c -> DayCommit(BASE_DATE.plusDays(i.toLong()), c) }

class CommitStatsTest {

    // --- lastN ---

    @Test
    fun `lastN returns whole list when shorter than n`() {
        val h = history(1, 2, 3)
        assertEquals(h, h.lastN(10))
    }

    @Test
    fun `lastN returns trailing n elements`() {
        val h = history(1, 2, 3, 4, 5)
        assertEquals(listOf(3, 4, 5), h.lastN(3).map { it.count })
    }

    @Test
    fun `lastN of empty list is empty`() {
        assertTrue(emptyList<DayCommit>().lastN(7).isEmpty())
    }

    // --- today ---

    @Test
    fun `today returns count of last day`() {
        val h = history(3, 7, 2)
        assertEquals(2, h.today())
    }

    @Test
    fun `today of empty history is zero`() {
        assertEquals(0, emptyList<DayCommit>().today())
    }

    // --- streak ---

    @Test
    fun `streak counts trailing consecutive days with commits`() {
        val h = history(1, 2, 0, 3, 4)
        assertEquals(2, h.streak())
    }

    @Test
    fun `streak is zero when today has no commits`() {
        val h = history(5, 5, 0)
        assertEquals(0, h.streak())
    }

    @Test
    fun `streak spans the whole history when every day has commits`() {
        val h = history(1, 1, 1, 1)
        assertEquals(4, h.streak())
    }

    @Test
    fun `streak of empty history is zero`() {
        assertEquals(0, emptyList<DayCommit>().streak())
    }

    // --- sum / maxCount ---

    @Test
    fun `sum adds every day's count`() {
        val h = history(1, 2, 3, 4)
        assertEquals(10, h.sum())
    }

    @Test
    fun `sum of empty history is zero`() {
        assertEquals(0, emptyList<DayCommit>().sum())
    }

    @Test
    fun `maxCount finds the busiest day`() {
        val h = history(1, 9, 3, 0)
        assertEquals(9, h.maxCount())
    }

    @Test
    fun `maxCount of empty history is zero`() {
        assertEquals(0, emptyList<DayCommit>().maxCount())
    }

    // --- weekOverWeek ---

    @Test
    fun `weekOverWeek treats a short history as entirely 'this week' with no comparison`() {
        val h = history(1, 1, 1) // < 7 days
        val wow = h.weekOverWeek()
        assertEquals(3, wow.thisWeek)
        assertEquals(0, wow.lastWeek)
        assertEquals(100, wow.percent)
        assertEquals(WeekDelta.Direction.UP, wow.direction)
        assertEquals("+100%", wow.label)
    }

    @Test
    fun `weekOverWeek is flat with no sign when both weeks are empty`() {
        val h = history(*IntArray(14))
        val wow = h.weekOverWeek()
        assertEquals(0, wow.thisWeek)
        assertEquals(0, wow.lastWeek)
        assertEquals(0, wow.percent)
        assertEquals(WeekDelta.Direction.FLAT, wow.direction)
        assertEquals("0%", wow.label)
    }

    @Test
    fun `weekOverWeek reports an upward trend with a plus sign`() {
        // previous 7 days sum to 10, last 7 days sum to 14 -> +40%
        val h = history(2, 1, 2, 1, 2, 1, 1, /* =10 */ 2, 2, 2, 2, 2, 2, 2 /* =14 */)
        val wow = h.weekOverWeek()
        assertEquals(10, wow.lastWeek)
        assertEquals(14, wow.thisWeek)
        assertEquals(40, wow.percent)
        assertEquals(WeekDelta.Direction.UP, wow.direction)
        assertEquals("+40%", wow.label)
    }

    @Test
    fun `weekOverWeek reports a downward trend with a minus sign`() {
        // previous 7 days sum to 10, last 7 days sum to 5 -> -50%
        val h = history(2, 1, 2, 1, 2, 1, 1, /* =10 */ 1, 1, 1, 1, 1, 0, 0 /* =5 */)
        val wow = h.weekOverWeek()
        assertEquals(10, wow.lastWeek)
        assertEquals(5, wow.thisWeek)
        assertEquals(-50, wow.percent)
        assertEquals(WeekDelta.Direction.DOWN, wow.direction)
        assertEquals("-50%", wow.label)
    }

    @Test
    fun `weekOverWeek treats a plus or minus one percent change as flat`() {
        // previous 7 days sum to 100, last 7 to 101 -> +1% is still FLAT (not UP)
        val h = history(
            15, 14, 14, 14, 14, 14, 15, // = 100
            15, 15, 14, 14, 14, 14, 15, // = 101
        )
        val wow = h.weekOverWeek()
        assertEquals(100, wow.lastWeek)
        assertEquals(101, wow.thisWeek)
        assertEquals(1, wow.percent)
        assertEquals(WeekDelta.Direction.FLAT, wow.direction)
    }

    // --- level ---

    @Test
    fun `level is zero for non-positive counts`() {
        assertEquals(0, level(0, 10))
        assertEquals(0, level(-3, 10))
    }

    @Test
    fun `level buckets counts into five intensity steps`() {
        assertEquals(1, level(2, 10))  // q = 0.2
        assertEquals(2, level(3, 10))  // q = 0.3
        assertEquals(2, level(5, 10))  // q = 0.5 (boundary, inclusive)
        assertEquals(3, level(6, 10))  // q = 0.6
        assertEquals(4, level(8, 10))  // q = 0.8
        assertEquals(4, level(10, 10)) // q = 1.0
    }

    @Test
    fun `level guards against a zero max instead of dividing by zero`() {
        assertEquals(4, level(1, 0))
    }

    // --- buildContributionGridDates ---

    @Test
    fun `contribution grid places today in its weekday row of the last column`() {
        val today = LocalDate.of(2024, 1, 3) // Wednesday
        val grid = buildContributionGridDates(today, columns = 3, rows = 7)

        assertEquals(7, grid.size)
        assertEquals(3, grid[0].size)

        val todayRow = 3 // Sunday=0 .. Wednesday=3 .. Saturday=6
        assertEquals(today, grid[todayRow].last())
    }

    @Test
    fun `contribution grid nulls out days after today within the current week`() {
        val today = LocalDate.of(2024, 1, 3) // Wednesday, row index 3
        val grid = buildContributionGridDates(today, columns = 3, rows = 7)

        // Thursday..Saturday (rows 4..6) of the current (last) week haven't happened yet.
        for (row in 4..6) {
            assertNull(grid[row].last())
        }
        // Sunday..Wednesday (rows 0..3) of the current week are real past/today dates.
        for (row in 0..3) {
            assertTrue(grid[row].last() != null)
        }
    }

    @Test
    fun `contribution grid walks backward one week per column`() {
        val today = LocalDate.of(2024, 1, 3) // Wednesday
        val grid = buildContributionGridDates(today, columns = 3, rows = 7)
        val todayRow = 3

        assertEquals(today, grid[todayRow][2])
        assertEquals(today.minusDays(7), grid[todayRow][1])
        assertEquals(today.minusDays(14), grid[todayRow][0])
    }

    // --- asDateMap ---

    @Test
    fun `asDateMap indexes counts by date`() {
        val h = history(4, 5, 6)
        val map = h.asDateMap()
        assertEquals(4, map[BASE_DATE])
        assertEquals(5, map[BASE_DATE.plusDays(1)])
        assertEquals(6, map[BASE_DATE.plusDays(2)])
        assertEquals(3, map.size)
    }
}
