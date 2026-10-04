package fr.aurelien.chrono

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GridLayoutTest {
    @get:Rule val ui = createComposeRule()

    @Test fun sixteenZonesKeepTotalsAndIntervalsVisibleWithLargerText() {
        val athletes = (1..16).map { Athlete(it, "Athlete $it") }
        val started = Stopwatch.start(AppData(athletes), 1_000, 7, "2026-10-04T10:00:00+02:00", "layout")
        val first = Stopwatch.record(started, 16, 2_349)
        val second = Stopwatch.record(first, 16, 3_500)
        val stopped = Stopwatch.stop(Stopwatch.record(second, 16, 5_000), 6_000)
        ui.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.3f)) {
                MaterialTheme {
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                        TimingScreen(stopped, false, {}, {}, {}, {}, {}, {}, {})
                    }
                }
            }
        }
        for (number in 1..16) {
            ui.onNodeWithTag("athlete-$number").assertIsDisplayed()
            ui.onNodeWithTag("total-$number", useUnmergedTree = true).assertIsDisplayed()
            ui.onNodeWithTag("interval-$number", useUnmergedTree = true).assertIsDisplayed()
        }
        ui.onNodeWithTag("shared-button").assertIsDisplayed()
        ui.onNodeWithTag("recent-16-1", useUnmergedTree = true).assertDoesNotExist()
    }

    private val sevenSplits = (1..7).map { Split(it, it * 12_300L, 12_300L) }

    @Test fun roomyZoneShowsThreeEarlierSplitsNewestFirstAndKeepsTheLatestProminent() {
        var taps = 0
        ui.setContent {
            MaterialTheme {
                AthleteZone(Athlete(1, "Alice"), sevenSplits, false, true,
                    Modifier.width(200.dp).height(280.dp)) { taps++ }
            }
        }
        ui.onNodeWithTag("total-1", useUnmergedTree = true).assertTextEquals("01:26.1").assertIsDisplayed()
        ui.onNodeWithTag("interval-1", useUnmergedTree = true).assertTextEquals("00:12.3").assertIsDisplayed()
        for (number in 4..6) ui.onNodeWithTag("recent-1-$number", useUnmergedTree = true).assertIsDisplayed()
        for (number in listOf(1, 2, 3, 7)) ui.onNodeWithTag("recent-1-$number", useUnmergedTree = true).assertDoesNotExist()
        val row6 = ui.onNodeWithTag("recent-1-6", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val row5 = ui.onNodeWithTag("recent-1-5", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val row4 = ui.onNodeWithTag("recent-1-4", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(row6.bottom <= row5.top && row5.bottom <= row4.top)
        ui.onNodeWithTag("recent-1-5", useUnmergedTree = true).performTouchInput { click() }
        ui.runOnIdle { assertEquals(1, taps) }
    }

    @Test fun narrowZoneWithLargeTextHidesHistoryAndKeepsBothLatestValuesVisible() {
        ui.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.3f)) {
                MaterialTheme {
                    AthleteZone(Athlete(1, "Alice"), sevenSplits, true, true,
                        Modifier.width(120.dp).height(280.dp)) {}
                }
            }
        }
        ui.onNodeWithTag("total-1", useUnmergedTree = true).assertIsDisplayed()
        ui.onNodeWithTag("interval-1", useUnmergedTree = true).assertIsDisplayed()
        for (number in 1..6) ui.onNodeWithTag("recent-1-$number", useUnmergedTree = true).assertDoesNotExist()
    }
}
