package fr.aurelien.chrono

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** Owns the current session. The UI only reads it and requests named actions. */
class ChronoViewModel(application: Application) : AndroidViewModel(application) {
    var data by mutableStateOf(AppData())
        private set
    var loaded by mutableStateOf(false)
        private set
    var loadError by mutableStateOf(false)
        private set
    var saveError by mutableStateOf(false)
        private set
    var exporting by mutableStateOf(false)
        private set
    var notice by mutableStateOf<String?>(null)
        private set

    var pendingSaves by mutableIntStateOf(0)
        private set

    private val store = SessionStore(application)
    // Serialize writes: an older split must never overwrite a newer Stop snapshot.
    private val saves = Channel<AppData>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            for (snapshot in saves) {
                saveError = try {
                    withContext(Dispatchers.IO) { store.save(snapshot) }
                    false
                } catch (_: Exception) {
                    true
                }
                pendingSaves--
            }
        }
        load()
    }

    fun load() {
        if (loaded) return
        loadError = false
        viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) { store.load() }
                val recovered = Stopwatch.recover(saved, SystemClock.elapsedRealtime(), bootCount())
                data = recovered
                loaded = true
                if (recovered != saved) enqueueSave(recovered)
            } catch (_: Exception) {
                // Keep an unreadable save intact rather than silently replacing the user's results.
                loadError = true
            }
        }
    }

    private fun bootCount(): Int? = try {
        Settings.Global.getInt(getApplication<Application>().contentResolver, Settings.Global.BOOT_COUNT)
            .takeIf { it >= 0 }
    } catch (_: Exception) {
        null
    }

    private fun update(snapshot: AppData) {
        if (!loaded || snapshot == data) return
        data = snapshot
        enqueueSave(snapshot)
    }

    private fun enqueueSave(snapshot: AppData) {
        pendingSaves++
        if (saves.trySend(snapshot).isFailure) {
            pendingSaves--
            saveError = true
        }
    }

    fun configure(names: List<String>) {
        if (!loaded || data.session != null) return
        update(Stopwatch.configure(data, names))
    }

    fun start() {
        val timestamp = SystemClock.elapsedRealtime()
        update(Stopwatch.start(data, timestamp, bootCount(), OffsetDateTime.now().toString(), UUID.randomUUID().toString()))
    }

    fun record(athleteNumber: Int) {
        val timestamp = SystemClock.elapsedRealtime()
        update(Stopwatch.record(data, athleteNumber, timestamp))
    }

    fun stop() {
        val timestamp = SystemClock.elapsedRealtime()
        update(Stopwatch.stop(data, timestamp))
    }

    fun newSession() = update(Stopwatch.newSession(data))

    fun retrySave() {
        enqueueSave(data)
    }

    fun exportFilename(): String {
        val started = data.session?.startedAt?.let { OffsetDateTime.parse(it) } ?: OffsetDateTime.now()
        return "chrono_${started.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.ROOT))}.csv"
    }

    fun export(uri: Uri) {
        val snapshot = data
        if (exporting || snapshot.session == null || snapshot.session.status == SessionStatus.RUNNING) return
        exporting = true
        viewModelScope.launch {
            notice = try {
                withContext(Dispatchers.IO) {
                    val csv = CsvExport.create(snapshot)
                    val output = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                        ?: error("The destination could not be opened.")
                    output.bufferedWriter(Charsets.UTF_8).use { it.write(csv) }
                }
                "CSV saved."
            } catch (_: Exception) {
                "Could not save the CSV. Your results are still available; please try again."
            }
            exporting = false
        }
    }

    fun clearNotice() { notice = null }
}
