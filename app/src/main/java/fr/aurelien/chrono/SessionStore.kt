package fr.aurelien.chrono

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.OffsetDateTime

/** One readable JSON file, atomically replaced so partial writes do not destroy the last save. */
class SessionStore(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "session.json"))

    fun load(): AppData {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return AppData()
        val root = JSONObject(file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
        require(root.getInt("version") == 1) { "Unsupported save format." }
        val athletes = root.getJSONArray("athletes").objects().map {
            Athlete(it.getInt("number"), it.getString("name"))
        }
        require(athletes.size in 1..16)
        require(athletes.map { it.number } == (1..athletes.size).toList())
        require(athletes.all { it.name.isNotBlank() })
        val saved = root.optJSONObject("session")
        val session = saved?.let {
            Session(
                id = it.getString("id"),
                startedAt = it.getString("startedAt"),
                startElapsedMs = it.getLong("startElapsedMs"),
                bootCount = if (it.isNull("bootCount")) null else it.getInt("bootCount"),
                status = SessionStatus.valueOf(it.getString("status")),
                taps = it.getJSONArray("taps").objects().map { tap ->
                    Tap(tap.getInt("athleteNumber"), tap.getLong("elapsedMs"))
                },
                stoppedAfterMs = if (it.isNull("stoppedAfterMs")) null else it.getLong("stoppedAfterMs"),
            ).also { value ->
                require(value.id.isNotBlank() && value.startElapsedMs >= 0)
                require(value.bootCount == null || value.bootCount >= 0)
                OffsetDateTime.parse(value.startedAt)
                require(value.taps.all { tap -> tap.athleteNumber in 1..athletes.size && tap.elapsedMs >= 0 })
                require(value.taps.zipWithNext().all { (a, b) -> a.elapsedMs <= b.elapsedMs })
                if (value.status == SessionStatus.STOPPED) {
                    require(value.stoppedAfterMs != null && value.stoppedAfterMs >= (value.taps.lastOrNull()?.elapsedMs ?: 0L))
                } else {
                    require(value.stoppedAfterMs == null)
                }
            }
        }
        return AppData(athletes, session)
    }

    fun save(data: AppData) {
        val root = JSONObject().put("version", 1).put("athletes", JSONArray().apply {
            data.athletes.forEach { put(JSONObject().put("number", it.number).put("name", it.name)) }
        }).put("session", data.session?.let { session ->
            JSONObject()
                .put("id", session.id)
                .put("startedAt", session.startedAt)
                .put("startElapsedMs", session.startElapsedMs)
                .put("bootCount", session.bootCount ?: JSONObject.NULL)
                .put("status", session.status.name)
                .put("stoppedAfterMs", session.stoppedAfterMs ?: JSONObject.NULL)
                .put("taps", JSONArray().apply {
                    session.taps.forEach { put(JSONObject().put("athleteNumber", it.athleteNumber).put("elapsedMs", it.elapsedMs)) }
                })
        } ?: JSONObject.NULL)
        val output = file.startWrite()
        try {
            output.write(root.toString(2).toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (exception: Exception) {
            file.failWrite(output)
            throw exception
        }
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
}
