package com.commitpulse.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Niedziela 27.09.2026 — „dzisiaj” we wszystkich testach. */
private val TODAY: LocalDate = LocalDate.of(2026, 9, 27)

/** Historia kończąca się dzisiaj: ostatni licznik = dzisiaj, przedostatni = wczoraj itd. */
private fun history(vararg counts: Int, endingOn: LocalDate = TODAY): List<DayCommit> =
    counts.mapIndexed { i, c -> DayCommit(endingOn.minusDays((counts.size - 1 - i).toLong()), c) }

class CommitStatsTest {

    // --- lastN / sum / maxCount / sumBetween ---

    @Test
    fun `lastN returns whole list when shorter than n`() {
        val h = history(1, 2, 3)
        assertEquals(h, h.lastN(10))
    }

    @Test
    fun `lastN returns trailing n elements`() {
        assertEquals(listOf(3, 4, 5), history(1, 2, 3, 4, 5).lastN(3).map { it.count })
    }

    @Test
    fun `sum adds every day's count and is zero for empty history`() {
        assertEquals(10, history(1, 2, 3, 4).sum())
        assertEquals(0, emptyList<DayCommit>().sum())
    }

    @Test
    fun `maxCount finds the busiest day and is zero for empty history`() {
        assertEquals(9, history(1, 9, 3, 0).maxCount())
        assertEquals(0, emptyList<DayCommit>().maxCount())
    }

    @Test
    fun `sumBetween includes both ends and ignores days outside the range`() {
        val h = history(1, 2, 3, 4, 5) // 23..27 wrz
        assertEquals(2 + 3 + 4, h.sumBetween(TODAY.minusDays(3), TODAY.minusDays(1)))
    }

    // --- summarize: dzisiaj ---

    @Test
    fun `today count is the count for today's date`() {
        assertEquals(2, summarize(history(3, 7, 2), TODAY).todayCount)
    }

    @Test
    fun `today count is zero when the last sync was on an earlier day`() {
        // Dane kończą się przedwczoraj — nie wolno pokazać przedwczorajszej liczby jako „dziś”.
        val stale = history(5, 9, endingOn = TODAY.minusDays(2))
        assertEquals(0, summarize(stale, TODAY).todayCount)
    }

    @Test
    fun `today count of empty history is zero`() {
        assertEquals(0, summarize(emptyList(), TODAY).todayCount)
    }

    // --- summarize: seria ---

    @Test
    fun `streak counts consecutive active days ending today`() {
        val s = summarize(history(1, 2, 0, 3, 4), TODAY)
        assertEquals(2, s.streak)
        assertEquals(TODAY.minusDays(1), s.streakStart)
    }

    @Test
    fun `streak is not broken by today having no contributions yet`() {
        // Rano, przed pierwszym commitem: seria z poprzednich dni wciąż trwa.
        val s = summarize(history(1, 1, 1, 0), TODAY)
        assertEquals(3, s.streak)
        assertEquals(TODAY.minusDays(3), s.streakStart)
    }

    @Test
    fun `streak is zero when neither today nor yesterday had contributions`() {
        val s = summarize(history(5, 5, 0, 0), TODAY)
        assertEquals(0, s.streak)
        assertNull(s.streakStart)
    }

    @Test
    fun `streak stops at a missing day in the data`() {
        val gap = listOf(
            DayCommit(TODAY.minusDays(3), 4),
            DayCommit(TODAY.minusDays(1), 2),
            DayCommit(TODAY, 1),
        )
        assertEquals(2, summarize(gap, TODAY).streak)
    }

    // --- summarize: okna 7-dniowe i trend ---

    @Test
    fun `last 7 and previous 7 are rolling windows by date`() {
        val s = summarize(history(2, 1, 2, 1, 2, 1, 1, /* =10 */ 2, 2, 2, 2, 2, 2, 2 /* =14 */), TODAY)
        assertEquals(14, s.last7)
        assertEquals(10, s.previous7)
        assertEquals(TODAY.minusDays(6), s.last7From)
        assertEquals(TODAY.minusDays(13), s.previous7From)
        assertEquals(TODAY.minusDays(7), s.previous7To)
    }

    @Test
    fun `windows use dates so gaps in the data do not shift the comparison`() {
        // Tylko dwa wpisy: dzisiaj (5) i 10 dni temu (3). Indeksowo „poprzedni tydzień” nie istnieje.
        val sparse = listOf(DayCommit(TODAY.minusDays(10), 3), DayCommit(TODAY, 5))
        val s = summarize(sparse, TODAY)
        assertEquals(5, s.last7)
        assertEquals(3, s.previous7)
    }

    @Test
    fun `a stale sync does not count old days as this week`() {
        val stale = history(*IntArray(14) { 1 }, endingOn = TODAY.minusDays(7))
        val s = summarize(stale, TODAY)
        assertEquals(0, s.last7)
        assertEquals(7, s.previous7)
    }

    @Test
    fun `trend reports the real numbers behind a huge percentage`() {
        // Scenariusz z widgetu: 6 → 35 to +483%, ale różnica +29 mówi, co się naprawdę stało.
        val s = summarize(history(1, 1, 1, 1, 1, 1, 0, /* =6 */ 5, 5, 5, 5, 5, 5, 5 /* =35 */), TODAY)
        assertEquals(29, s.trend.diff)
        assertEquals(483, s.trend.percent)
        assertEquals("+29", s.trend.label(TrendFormat.DIFF))
        assertEquals("+483%", s.trend.label(TrendFormat.PERCENT))
        assertEquals(TrendDirection.UP, s.trend.direction)
    }

    // --- Trend ---

    @Test
    fun `trend from zero has no percentage instead of a fake +100 percent`() {
        val t = Trend(current = 4, previous = 0)
        assertNull(t.percent)
        assertNull(t.percentLabel)
        assertEquals("+4", t.label(TrendFormat.PERCENT))
    }

    @Test
    fun `trend down uses a minus sign`() {
        val t = Trend(current = 5, previous = 10)
        assertEquals(-5, t.diff)
        assertEquals(-50, t.percent)
        assertEquals("−5", t.diffLabel)
        assertEquals("−50%", t.percentLabel)
        assertEquals(TrendDirection.DOWN, t.direction)
    }

    @Test
    fun `trend with no change is flat`() {
        val t = Trend(current = 7, previous = 7)
        assertEquals(TrendDirection.FLAT, t.direction)
        assertEquals("0", t.diffLabel)
        assertEquals("0%", t.percentLabel)
    }

    @Test
    fun `any real change counts as a direction even when the percentage rounds small`() {
        assertEquals(TrendDirection.UP, Trend(current = 101, previous = 100).direction)
        assertEquals(1, Trend(current = 101, previous = 100).percent)
    }

    @Test
    fun `both empty periods are flat with zero difference`() {
        val t = Trend(0, 0)
        assertEquals(TrendDirection.FLAT, t.direction)
        assertEquals("0", t.label(TrendFormat.PERCENT))
    }

    // --- Tydzień kalendarzowy (podsumowanie tygodnia) ---

    @Test
    fun `last complete week is the previous Monday to Sunday`() {
        val monday = LocalDate.of(2026, 9, 28)
        val week = lastCompleteWeek(emptyList(), monday)
        assertEquals(LocalDate.of(2026, 9, 21), week.weekStart)
        assertEquals(LocalDate.of(2026, 9, 27), week.weekEnd)
        assertEquals(DayOfWeek.MONDAY, week.weekStart.dayOfWeek)
    }

    @Test
    fun `last complete week on a Sunday is still the week before`() {
        val week = lastCompleteWeek(emptyList(), TODAY) // niedziela 27.09 — bieżący tydzień jeszcze trwa
        assertEquals(LocalDate.of(2026, 9, 14), week.weekStart)
    }

    @Test
    fun `last complete week compares against the week before it, not against zero`() {
        // Regresja: dawniej trend liczony był na liście 7 dni, więc zawsze wychodziło „+100%”.
        val monday = LocalDate.of(2026, 9, 28)
        val h = history(*IntArray(14) { if (it < 7) 1 else 2 }, endingOn = monday.minusDays(1))
        val week = lastCompleteWeek(h, monday)
        assertEquals(14, week.trend.current)
        assertEquals(7, week.trend.previous)
        assertEquals(100, week.trend.percent)
    }

    // --- plural ---

    @Test
    fun `polish plural forms`() {
        assertEquals("kontrybucja", plural(1, "kontrybucja", "kontrybucje", "kontrybucji"))
        assertEquals("kontrybucje", plural(3, "kontrybucja", "kontrybucje", "kontrybucji"))
        assertEquals("kontrybucji", plural(5, "kontrybucja", "kontrybucje", "kontrybucji"))
        assertEquals("kontrybucji", plural(12, "kontrybucja", "kontrybucje", "kontrybucji"))
        assertEquals("kontrybucje", plural(22, "kontrybucja", "kontrybucje", "kontrybucji"))
        assertEquals("kontrybucji", plural(0, "kontrybucja", "kontrybucje", "kontrybucji"))
    }

    // --- level ---

    @Test
    fun `level is zero for non-positive counts`() {
        assertEquals(0, level(0, 10))
        assertEquals(0, level(-3, 10))
    }

    @Test
    fun `level buckets counts into five intensity steps`() {
        assertEquals(1, level(2, 10))
        assertEquals(2, level(3, 10))
        assertEquals(2, level(5, 10))
        assertEquals(3, level(6, 10))
        assertEquals(4, level(8, 10))
        assertEquals(4, level(10, 10))
    }

    @Test
    fun `level guards against a zero max instead of dividing by zero`() {
        assertEquals(4, level(1, 0))
    }

    // --- buildContributionGridDates ---

    @Test
    fun `contribution grid places today in its weekday row of the last column`() {
        val wednesday = LocalDate.of(2024, 1, 3)
        val grid = buildContributionGridDates(wednesday, columns = 3, rows = 7)
        assertEquals(7, grid.size)
        assertEquals(3, grid[0].size)
        assertEquals(wednesday, grid[3].last())
    }

    @Test
    fun `contribution grid nulls out days after today within the current week`() {
        val grid = buildContributionGridDates(LocalDate.of(2024, 1, 3), columns = 3, rows = 7)
        for (row in 4..6) assertNull(grid[row].last())
        for (row in 0..3) assertTrue(grid[row].last() != null)
    }

    @Test
    fun `contribution grid walks backward one week per column`() {
        val wednesday = LocalDate.of(2024, 1, 3)
        val grid = buildContributionGridDates(wednesday, columns = 3, rows = 7)
        assertEquals(wednesday, grid[3][2])
        assertEquals(wednesday.minusDays(7), grid[3][1])
        assertEquals(wednesday.minusDays(14), grid[3][0])
    }

    // --- asDateMap ---

    @Test
    fun `asDateMap indexes counts by date`() {
        val map = history(4, 5, 6).asDateMap()
        assertEquals(4, map[TODAY.minusDays(2)])
        assertEquals(6, map[TODAY])
        assertEquals(3, map.size)
    }
}
