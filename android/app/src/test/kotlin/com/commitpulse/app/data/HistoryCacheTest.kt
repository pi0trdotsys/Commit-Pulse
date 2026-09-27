package com.commitpulse.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryCacheTest {

    @Test
    fun `toJson then toDayCommitList round-trips the history`() {
        val original = listOf(
            DayCommit(LocalDate.of(2024, 1, 1), 3),
            DayCommit(LocalDate.of(2024, 1, 2), 0),
            DayCommit(LocalDate.of(2024, 1, 3), 7),
        )

        val restored = original.toJson().toDayCommitList()

        assertEquals(original, restored)
    }

    @Test
    fun `toDayCommitList sorts entries by date regardless of input order`() {
        val json = """[{"date":"2024-01-03","count":1},{"date":"2024-01-01","count":2},{"date":"2024-01-02","count":3}]"""

        val restored = json.toDayCommitList()

        assertEquals(
            listOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 3)),
            restored.map { it.date },
        )
    }

    @Test
    fun `toDayCommitList returns an empty list for malformed json`() {
        assertTrue("not json at all".toDayCommitList().isEmpty())
        assertTrue("".toDayCommitList().isEmpty())
        assertTrue("""[{"date":"not-a-date","count":1}]""".toDayCommitList().isEmpty())
    }

    @Test
    fun `toJson of an empty history round-trips to an empty list`() {
        assertTrue(emptyList<DayCommit>().toJson().toDayCommitList().isEmpty())
    }
}
