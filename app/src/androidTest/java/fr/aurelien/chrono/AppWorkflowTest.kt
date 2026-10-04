package fr.aurelien.chrono

import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppWorkflowTest {
    @get:Rule val ui = createEmptyComposeRule()
    private lateinit var activity: ActivityScenario<MainActivity>
    private lateinit var model: ChronoViewModel

    @Before fun openFreshApp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        listOf("session.json", "session.json.bak", "session.json.new").forEach { context.deleteFile(it) }
        activity = ActivityScenario.launch(MainActivity::class.java)
        activity.onActivity { model = ViewModelProvider(it)[ChronoViewModel::class.java] }
        ui.waitUntil(10_000) { model.loaded }
    }

    @After fun closeApp() {
        ui.waitUntil(10_000) { model.pendingSaves == 0 }
        activity.close()
    }

    @Test fun setupSharedStartIndependentSplitsStopAndCancelledReset() {
        ui.onNodeWithTag("name-1").performTextReplacement("Alice")
        ui.onNodeWithTag("ready").performClick()
        ui.onNodeWithText("Start").performClick()
        ui.onNodeWithTag("athlete-1").performClick()
        ui.onNodeWithTag("athlete-2").performClick()
        ui.onNodeWithTag("athlete-1").performClick()
        ui.onNodeWithText("Stop").performClick()
        ui.runOnIdle {
            assertEquals("Alice", model.data.athletes.first().name)
            assertEquals(2, model.data.session!!.splitsFor(1).size)
            assertEquals(1, model.data.session!!.splitsFor(2).size)
            assertEquals(SessionStatus.STOPPED, model.data.session!!.status)
        }
        ui.onNodeWithText("New session").performClick()
        ui.onNodeWithText("Keep results").performClick()
        ui.onNodeWithText("Results").performClick()
        ui.onNodeWithText("1. Alice").assertIsDisplayed()
        ui.onNodeWithText("Back").performClick()
        ui.onNodeWithText("New session").performClick()
        ui.onNodeWithText("Clear results").performClick()
        ui.onNodeWithTag("name-1").assertTextContains("Alice")
        ui.runOnIdle { assertNull(model.data.session) }
    }

    @Test fun allSixteenZonesAndTheSharedButtonFitWithoutScrolling() {
        repeat(8) { ui.onNodeWithTag("increase-count").performClick() }
        ui.onNodeWithTag("athlete-count").assertTextEquals("16")
        ui.onNodeWithTag("increase-count").assertIsNotEnabled()
        ui.onNodeWithTag("ready").performClick()
        for (number in 1..16) ui.onNodeWithTag("athlete-$number").assertIsDisplayed()
        ui.onNodeWithTag("shared-button").assertIsDisplayed().assertIsEnabled()
        ui.onNodeWithText("Start").performClick()
        ui.onNodeWithTag("athlete-16").performClick()
        ui.onNodeWithText("Stop").performClick()
        ui.runOnIdle { assertEquals(1, model.data.session!!.splitsFor(16).size) }
    }

    @Test fun backgroundAndActivityRecreationPreserveTimingAndSplits() {
        ui.onNodeWithTag("ready").performClick()
        ui.onNodeWithText("Start").performClick()
        ui.onNodeWithTag("athlete-1").performClick()
        activity.moveToState(Lifecycle.State.CREATED)
        SystemClock.sleep(350)
        activity.moveToState(Lifecycle.State.RESUMED)
        activity.recreate()
        ui.onNodeWithTag("athlete-1").performClick()
        ui.onNodeWithText("Stop").performClick()
        ui.runOnIdle {
            val splits = model.data.session!!.splitsFor(1)
            assertEquals(2, splits.size)
            assertTrue(splits.last().intervalMs >= 350)
            assertTrue(model.data.session!!.stoppedAfterMs!! >= splits.last().totalMs)
        }
    }
}
