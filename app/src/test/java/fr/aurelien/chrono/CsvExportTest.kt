package fr.aurelien.chrono

import org.junit.Assert.*
import org.junit.Test

class CsvExportTest {
    private fun running(names: List<String> = listOf("Alice", "Bob")): AppData = Stopwatch.start(
        Stopwatch.configure(AppData(), names), 1_000, 7, "2026-10-04T10:00:00+02:00", "s1",
    )

    @Test fun csvContainsBothTimesAndAStopRowForAthletesWithoutTaps() {
        var data = Stopwatch.record(running(), 1, 13_300)
        data = Stopwatch.record(data, 1, 26_100)
        data = Stopwatch.stop(data, 31_000)
        val csv = CsvExport.create(data)
        assertTrue(csv.contains("1,\"Alice\",split,1,12.3,12.3\r\n"))
        assertTrue(csv.contains("1,\"Alice\",split,2,25.1,12.8\r\n"))
        assertTrue(csv.contains("1,\"Alice\",stop,,30.0,\r\n"))
        assertTrue(csv.contains("2,\"Bob\",stop,,30.0,\r\n"))
        assertEquals(5, csv.trimEnd().lines().size)
    }

    @Test fun namesWithQuotesCommasNewlinesAndUnicodeAreEscaped() {
        val csv = CsvExport.create(Stopwatch.stop(running(listOf("Zoë, \"A\"\nTeam")), 2_000))
        assertTrue(csv.contains("\"Zoë, \"\"A\"\"\nTeam\""))
    }

    @Test fun spreadsheetFormulasAreNeutralizedIncludingLeadingWhitespace() {
        val csv = CsvExport.create(Stopwatch.stop(running(listOf("=1+1", "+SUM(A1)", "-3", "@name", "  =2+2")), 2_000))
        for (name in listOf("=1+1", "+SUM(A1)", "-3", "@name", "=2+2")) {
            assertTrue(csv.contains("\"'$name\""))
        }
    }

    @Test fun interruptedSessionHasSavedSplitsButNoInventedStopRows() {
        val interrupted = Stopwatch.recover(Stopwatch.record(running(), 1, 2_000), 100, 8)
        val csv = CsvExport.create(interrupted)
        assertTrue(csv.contains(",split,1,1.0,1.0"))
        assertFalse(csv.contains(",stop,"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun runningSessionCannotBeExported() {
        CsvExport.create(running())
    }
}
