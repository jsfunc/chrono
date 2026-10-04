package fr.aurelien.chrono

/** Immutable records: every action creates a new snapshot for the screen and storage. */
data class Athlete(val number: Int, val name: String)
data class Tap(val athleteNumber: Int, val elapsedMs: Long)
data class Split(val number: Int, val totalMs: Long, val intervalMs: Long)
enum class SessionStatus { RUNNING, STOPPED, INTERRUPTED }

data class Session(
    val id: String,
    val startedAt: String,
    val startElapsedMs: Long,
    val bootCount: Int?,
    val status: SessionStatus = SessionStatus.RUNNING,
    val taps: List<Tap> = emptyList(),
    val stoppedAfterMs: Long? = null,
) {
    fun splitsFor(athleteNumber: Int): List<Split> {
        var previous = 0L
        return taps.filter { it.athleteNumber == athleteNumber }.mapIndexed { index, tap ->
            Split(index + 1, tap.elapsedMs, tap.elapsedMs - previous).also {
                previous = tap.elapsedMs
            }
        }
    }
}

data class AppData(
    val athletes: List<Athlete> = (1..8).map { Athlete(it, "Athlete $it") },
    val session: Session? = null,
)

/** No Android dependencies here: the clock is supplied by the caller, including in tests. */
object Stopwatch {
    fun configure(data: AppData, names: List<String>): AppData {
        require(data.session == null) { "Finish and clear the session before editing athletes." }
        require(names.size in 1..16)
        return data.copy(athletes = names.mapIndexed { index, name ->
            Athlete(index + 1, name.trim().ifBlank { "Athlete ${index + 1}" })
        })
    }

    fun start(data: AppData, nowMs: Long, bootCount: Int?, startedAt: String, id: String): AppData {
        if (data.session != null) return data
        return data.copy(session = Session(id, startedAt, nowMs, bootCount))
    }

    fun record(data: AppData, athleteNumber: Int, nowMs: Long): AppData {
        val session = data.session ?: return data
        if (session.status != SessionStatus.RUNNING) return data
        if (data.athletes.none { it.number == athleteNumber }) return data
        val elapsed = nowMs - session.startElapsedMs
        if (elapsed < 0 || elapsed < (session.taps.lastOrNull()?.elapsedMs ?: 0L)) return data
        return data.copy(session = session.copy(taps = session.taps + Tap(athleteNumber, elapsed)))
    }

    fun stop(data: AppData, nowMs: Long): AppData {
        val session = data.session ?: return data
        if (session.status != SessionStatus.RUNNING) return data
        val elapsed = maxOf(0L, nowMs - session.startElapsedMs, session.taps.lastOrNull()?.elapsedMs ?: 0L)
        return data.copy(session = session.copy(status = SessionStatus.STOPPED, stoppedAfterMs = elapsed))
    }

    fun newSession(data: AppData): AppData {
        if (data.session?.status == SessionStatus.RUNNING) return data
        return data.copy(session = null)
    }

    fun recover(data: AppData, nowMs: Long, bootCount: Int?): AppData {
        val session = data.session ?: return data
        if (session.status != SessionStatus.RUNNING) return data
        // A reboot resets the monotonic clock. Unknown boot identity is also unsafe to resume.
        val sameBoot = bootCount != null && session.bootCount != null && bootCount == session.bootCount
        val plausibleTime = nowMs >= session.startElapsedMs + (session.taps.lastOrNull()?.elapsedMs ?: 0L)
        return if (sameBoot && plausibleTime) data else data.copy(
            session = session.copy(status = SessionStatus.INTERRUPTED, stoppedAfterMs = null),
        )
    }
}

private fun roundedTenths(milliseconds: Long): Long {
    val value = milliseconds.coerceAtLeast(0L)
    return value / 100 + if (value % 100 >= 50) 1 else 0
}

fun formatDuration(milliseconds: Long): String {
    val tenths = roundedTenths(milliseconds)
    val minutes = tenths / 600
    val seconds = (tenths / 10) % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${tenths % 10}"
}

fun formatSeconds(milliseconds: Long): String {
    val tenths = roundedTenths(milliseconds)
    return "${tenths / 10}.${tenths % 10}"
}
