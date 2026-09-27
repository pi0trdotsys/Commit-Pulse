package com.commitpulse.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLayoutTest {

    private val sunday = LocalDate.of(2026, 9, 27)
    private val wednesday = LocalDate.of(2026, 9, 23)

    // --- computeGridLayout: nic nie może wyjść poza dostępny obszar ---

    @Test
    fun `grid always fits inside the available area for every widget size`() {
        for (w in 20..420 step 7) {
            for (h in 20..420 step 7) {
                for (fitAll in listOf(false, true)) {
                    val maxDays = if (fitAll) 30 else 140
                    val layout = computeGridLayout(w.toFloat(), h.toFloat(), maxDays, sunday, fitAll)
                    assertTrue("width ${layout.widthDp} > $w at ${w}x$h fitAll=$fitAll", layout.widthDp <= w + 0.01f)
                    assertTrue("height ${layout.heightDp} > $h at ${w}x$h fitAll=$fitAll", layout.heightDp <= h + 0.01f)
                    assertTrue(layout.weeks >= 1)
                }
            }
        }
    }

    @Test
    fun `narrow tall 1x2 widget switches to calendar rows and shows many weeks`() {
        // Rzeczywisty obszar siatki widgetu 1x2 zmierzony na telefonie (60.7x172dp minus padding i nagłówek).
        val layout = computeGridLayout(48.7f, 104f, maxDays = 140, today = sunday)

        assertEquals(GridOrientation.WEEKS_AS_ROWS, layout.orientation)
        assertEquals(7, layout.columns)
        assertTrue("expected >= 10 weeks, got ${layout.weeks}", layout.weeks >= 10)
    }

    @Test
    fun `wide 2x1 widget keeps the GitHub layout with weeks as columns`() {
        val layout = computeGridLayout(160f, 56f, maxDays = 140, today = sunday)

        assertEquals(GridOrientation.WEEKS_AS_COLUMNS, layout.orientation)
        assertEquals(7, layout.rows)
        assertTrue(layout.weeks >= 15)
    }

    @Test
    fun `fitAllDays shows the whole 30 day range even in a narrow widget`() {
        val layout = computeGridLayout(48.7f, 130f, maxDays = 30, today = sunday, fitAllDays = true)

        val slots = layout.weeks * 7
        assertTrue("only $slots slots for 30 days", slots >= 30)
        assertTrue(layout.widthDp <= 48.7f + 0.01f)
    }

    @Test
    fun `cells never exceed the configured maximum size in huge widgets`() {
        val layout = computeGridLayout(400f, 400f, maxDays = 30, today = sunday, fitAllDays = true, maxStrideDp = 22f)

        assertTrue(layout.strideDp <= 22f)
    }

    // --- weeksToCover ---

    @Test
    fun `weeksToCover accounts for a partial current week`() {
        // Niedziela przy tygodniu od niedzieli: bieżący tydzień ma 1 dzień -> 1 + ceil(29/7) = 6.
        assertEquals(6, weeksToCover(30, sunday, DayOfWeek.SUNDAY))
        // Niedziela przy tygodniu od poniedziałku: bieżący tydzień jest pełny -> 1 + ceil(23/7) = 5.
        assertEquals(5, weeksToCover(30, sunday, DayOfWeek.MONDAY))
        assertEquals(1, weeksToCover(1, wednesday, DayOfWeek.SUNDAY))
    }

    // --- buildGridCells ---

    @Test
    fun `calendar rows start on Monday and end with today`() {
        val layout = GridLayout(GridOrientation.WEEKS_AS_ROWS, weeks = 3, strideDp = 6f, gapRatio = 0.2f)
        val cells = buildGridCells(sunday, layout)

        assertEquals(3, cells.size)
        cells.forEach { assertEquals(7, it.size) }
        assertEquals(sunday, cells.last().last())
        assertEquals(DayOfWeek.MONDAY, cells.last().first()!!.dayOfWeek)
        assertEquals(sunday.minusDays(7), cells[1].last())
    }

    @Test
    fun `calendar rows leave the rest of the current week empty`() {
        val layout = GridLayout(GridOrientation.WEEKS_AS_ROWS, weeks = 2, strideDp = 6f, gapRatio = 0.2f)
        val cells = buildGridCells(wednesday, layout)

        assertEquals(wednesday, cells.last()[2]) // Pn, Wt, Śr
        for (i in 3..6) assertNull(cells.last()[i])
    }

    @Test
    fun `github columns orientation matches buildContributionGridDates`() {
        val layout = GridLayout(GridOrientation.WEEKS_AS_COLUMNS, weeks = 4, strideDp = 8f, gapRatio = 0.2f)

        assertEquals(buildContributionGridDates(sunday, 4, 7), buildGridCells(sunday, layout))
    }

    @Test
    fun `days before the earliest available date are not drawn`() {
        val layout = GridLayout(GridOrientation.WEEKS_AS_COLUMNS, weeks = 6, strideDp = 8f, gapRatio = 0.2f)
        val earliest = sunday.minusDays(29)
        val cells = buildGridCells(sunday, layout, earliest)

        val drawn = cells.flatten().filterNotNull()
        assertEquals(30, drawn.size)
        assertTrue(drawn.all { !it.isBefore(earliest) })
    }

    // --- buildContributionGridDates z innym pierwszym dniem tygodnia ---

    @Test
    fun `monday-first grid puts sunday in the last row`() {
        val grid = buildContributionGridDates(sunday, columns = 1, rows = 7, firstDayOfWeek = DayOfWeek.MONDAY)

        assertEquals(sunday, grid[6][0])
        assertEquals(sunday.minusDays(6), grid[0][0])
    }

    // --- Tekst i nagłówek ---

    @Test
    fun `text width estimate grows with length, size and font scale`() {
        val base = estimateTextWidthDp("12", 10f)
        assertTrue(estimateTextWidthDp("123", 10f) > base)
        assertTrue(estimateTextWidthDp("12", 14f) > base)
        assertTrue(estimateTextWidthDp("12", 10f, fontScale = 1.3f) > base)
    }

    @Test
    fun `emoji are estimated wider than digits`() {
        assertTrue(estimateTextWidthDp("🔥", 10f) > estimateTextWidthDp("1", 10f))
    }

    @Test
    fun `header uses one row when everything fits`() {
        assertEquals(HeaderLayout.ONE_ROW, chooseHeaderLayout(200f, 40f, 40f, 40f))
    }

    @Test
    fun `header drops the delta to a second row when needed`() {
        assertEquals(HeaderLayout.TWO_ROWS, chooseHeaderLayout(90f, 40f, 40f, 40f))
    }

    @Test
    fun `header stacks three rows in a very narrow widget`() {
        assertEquals(HeaderLayout.THREE_ROWS, chooseHeaderLayout(48f, 30f, 30f, 30f))
    }

    // --- Wybrana liczba tygodni ---

    @Test
    fun `fixed weeks shows exactly that many weeks when they fit`() {
        for (weeks in listOf(4, 8, 12, 16)) {
            val layout = computeGridLayout(160f, 56f, maxDays = 364, today = sunday, fixedWeeks = weeks)
            assertEquals(weeks, layout.weeks)
        }
    }

    @Test
    fun `fewer fixed weeks give bigger squares`() {
        val four = computeGridLayout(160f, 56f, maxDays = 364, today = sunday, fixedWeeks = 4)
        val sixteen = computeGridLayout(160f, 56f, maxDays = 364, today = sunday, fixedWeeks = 16)
        assertTrue(four.strideDp > sixteen.strideDp)
    }

    @Test
    fun `fixed weeks picks the orientation with bigger squares`() {
        // Wąski, wysoki widget (48.7x104dp):
        // 4 tygodnie jako kolumny -> krok min(104/6.8, 48.7/3.8) = 12.8dp,
        // jako wiersze -> krok 48.7/6.8 = 7.2dp. Wygrywają większe kwadraty.
        val few = computeGridLayout(48.7f, 104f, maxDays = 364, today = sunday, fixedWeeks = 4)
        assertEquals(4, few.weeks)
        assertEquals(GridOrientation.WEEKS_AS_COLUMNS, few.orientation)
        assertEquals(12.8f, few.strideDp, 0.1f)

        // 12 tygodni jako kolumny ledwo by weszło (krok 4dp), jako wiersze krok 7.2dp.
        val many = computeGridLayout(48.7f, 104f, maxDays = 364, today = sunday, fixedWeeks = 12)
        assertEquals(12, many.weeks)
        assertEquals(GridOrientation.WEEKS_AS_ROWS, many.orientation)
    }

    @Test
    fun `too many fixed weeks for the space still fits instead of overflowing`() {
        val layout = computeGridLayout(48.7f, 104f, maxDays = 364, today = sunday, fixedWeeks = 52)
        assertTrue(layout.weeks in 1..52)
        assertTrue(layout.widthDp <= 48.7f + 0.01f)
        assertTrue(layout.heightDp <= 104f + 0.01f)
    }

    @Test
    fun `fixed weeks always fit for every widget size`() {
        for (weeks in GRID_WEEK_OPTIONS.filter { it > 0 }) {
            for (w in 20..420 step 20) {
                for (h in 20..420 step 20) {
                    val layout = computeGridLayout(w.toFloat(), h.toFloat(), 364, sunday, fixedWeeks = weeks)
                    assertTrue(layout.widthDp <= w + 0.01f && layout.heightDp <= h + 0.01f)
                }
            }
        }
    }

    // --- Daty i podsumowanie tygodnia ---

    @Test
    fun `date range within a month shares the month name`() {
        assertEquals("21–27 wrz", formatRange(LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 27)))
    }

    @Test
    fun `date range across months names both months`() {
        assertEquals("29 wrz – 5 paź", formatRange(LocalDate.of(2026, 9, 29), LocalDate.of(2026, 10, 5)))
    }

    @Test
    fun `weekly digest text states the real numbers`() {
        val week = CalendarWeekSummary(LocalDate.of(2026, 9, 21), Trend(current = 34, previous = 6))
        val text = weeklyDigestText(week)
        assertEquals("Miniony tydzień (21–27 wrz): 34 kontrybucje", text.headline)
        assertEquals("+28 vs tydzień wcześniej (6)", text.comparison)
    }

    @Test
    fun `weekly digest text for an unchanged week`() {
        val text = weeklyDigestText(CalendarWeekSummary(LocalDate.of(2026, 9, 21), Trend(5, 5)))
        assertEquals("tyle samo co tydzień wcześniej (5)", text.comparison)
    }
}
