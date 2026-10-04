package fr.aurelien.chrono

import org.junit.Assert.*
import org.junit.Test

class StopwatchTest {
    private fun running(): AppData = Stopwatch.start(
        Stopwatch.configure(AppData(), listOf("Alice", "Bob")),
        1_000L, 7, "2026-10-04T10:00:00+02:00", "test-session",
    )

    @Test fun athletesShareStartAndHaveIndependentIntervals() {
        var data = running()
        data = Stopwatch.record(data, 1, 13_300)
        data = Stopwatch.record(data, 2, 17_000)
        data = Stopwatch.record(data, 1, 26_100)
        assertEquals(listOf(Split(1, 12_300, 12_300), Split(2, 25_100, 12_800)), data.session!!.splitsFor(1))
        assertEquals(listOf(Split(1, 16_000, 16_000)), data.session!!.splitsFor(2))
    }

    @Test fun stopFreezesEveryoneWithoutAddingSplits() {
        val data = Stopwatch.stop(Stopwatch.record(running(), 1, 2_000), 31_000)
        assertEquals(SessionStatus.STOPPED, data.session!!.status)
        assertEquals(30_000L, data.session!!.stoppedAfterMs)
        assertEquals(1, data.session!!.taps.size)
        assertEquals(data, Stopwatch.record(data, 2, 32_000))
        assertEquals(data, Stopwatch.stop(data, 40_000))
    }

    @Test fun repeatedStartDoesNotChangeTheReferenceTime() {
        val data = running()
        assertEquals(data, Stopwatch.start(data, 9_000, 7, "different", "different"))
    }

    @Test fun rapidTapsAreRecordedWithoutDebouncing() {
        val data = Stopwatch.record(Stopwatch.record(running(), 1, 1_010), 1, 1_010)
        assertEquals(listOf(Split(1, 10, 10), Split(2, 10, 0)), data.session!!.splitsFor(1))
    }

    @Test fun readyInvalidAthleteAndBackwardsClockCannotAddRecords() {
        assertEquals(AppData(), Stopwatch.record(AppData(), 1, 1_000))
        val data = Stopwatch.record(running(), 1, 2_000)
        assertEquals(data, Stopwatch.record(data, 3, 3_000))
        assertEquals(data, Stopwatch.record(data, 2, 1_999))
    }

    @Test fun newSessionKeepsNamesAndCannotClearRunningResults() {
        val data = running()
        assertEquals(data, Stopwatch.newSession(data))
        val reset = Stopwatch.newSession(Stopwatch.stop(data, 5_000))
        assertNull(reset.session)
        assertEquals(data.athletes, reset.athletes)
    }

    @Test fun blankAndDuplicateNamesHaveStableAthleteNumbers() {
        val data = Stopwatch.configure(AppData(), listOf(" ", "Alice", "Alice"))
        assertEquals(listOf(Athlete(1, "Athlete 1"), Athlete(2, "Alice"), Athlete(3, "Alice")), data.athletes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun moreThanSixteenAthletesIsRejected() {
        Stopwatch.configure(AppData(), List(17) { "Athlete" })
    }

    @Test fun recoveryOnSameBootIncludesTimeSpentAsleep() {
        val saved = Stopwatch.record(running(), 1, 2_000)
        val recovered = Stopwatch.recover(saved, 601_000, 7)
        assertEquals(SessionStatus.RUNNING, recovered.session!!.status)
        val tapped = Stopwatch.record(recovered, 1, 601_000)
        assertEquals(Split(2, 600_000, 599_000), tapped.session!!.splitsFor(1).last())
    }

    @Test fun rebootWithEvenALargerClockInterruptsTheSessionAndPreservesSplits() {
        val saved = Stopwatch.record(running(), 1, 2_000)
        val recovered = Stopwatch.recover(saved, 9_000_000, 8)
        assertEquals(SessionStatus.INTERRUPTED, recovered.session!!.status)
        assertEquals(saved.session!!.taps, recovered.session!!.taps)
        assertNull(recovered.session!!.stoppedAfterMs)
    }

    @Test fun unknownBootAndImplausibleClockAreNotResumed() {
        assertEquals(SessionStatus.INTERRUPTED, Stopwatch.recover(running(), 3_000, null).session!!.status)
        assertEquals(SessionStatus.INTERRUPTED, Stopwatch.recover(running(), 500, 7).session!!.status)
        val saved = Stopwatch.record(running(), 1, 2_000)
        assertEquals(SessionStatus.INTERRUPTED, Stopwatch.recover(saved, 1_999, 7).session!!.status)
    }

    @Test fun stoppedSessionDoesNotDependOnCurrentBoot() {
        val stopped = Stopwatch.stop(running(), 6_000)
        assertEquals(stopped, Stopwatch.recover(stopped, 100, 99))
    }

    @Test fun tenthsRoundAtBoundariesAndMinutesCanExceedFiftyNine() {
        assertEquals("00:00.0", formatDuration(49))
        assertEquals("00:00.1", formatDuration(50))
        assertEquals("01:00.0", formatDuration(59_950))
        assertEquals("60:00.0", formatDuration(3_600_000))
        assertEquals("12.3", formatSeconds(12_349))
        assertEquals("12.4", formatSeconds(12_350))
    }
}
