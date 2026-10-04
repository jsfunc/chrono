package fr.aurelien.chrono

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class SessionStoreTest {
    private lateinit var folder: File
    private lateinit var store: SessionStore

    @Before fun createIsolatedStore() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        folder = File(context.cacheDir, "store-test-${System.nanoTime()}").apply { mkdirs() }
        store = SessionStore(object : ContextWrapper(context) {
            override fun getFilesDir(): File = folder
        })
    }

    @After fun removeTestFiles() { folder.deleteRecursively() }

    @Test fun stoppedSessionRoundTripsWithFullNamesAndMillisecondValues() {
        val configured = Stopwatch.configure(AppData(), listOf("Zoë, \"A\"", "Bob"))
        val running = Stopwatch.start(configured, 1_000, 7, "2026-10-04T10:00:00+02:00", "s1")
        val saved = Stopwatch.stop(Stopwatch.record(running, 1, 2_349), 5_678)
        store.save(saved)
        assertEquals(saved, store.load())
    }

    @Test fun unknownBootIdentityIsStoredAsNull() {
        val saved = Stopwatch.start(AppData(), 1_000, null, "2026-10-04T10:00:00+02:00", "s1")
        store.save(saved)
        assertEquals(saved, store.load())
    }

    @Test fun corruptSaveIsReportedAndLeftIntact() {
        val file = File(folder, "session.json").apply { writeText("invalid JSON") }
        assertThrows(Exception::class.java) { store.load() }
        assertEquals("invalid JSON", file.readText())
    }
}
