package fr.aurelien.chrono

object CsvExport {
    fun create(data: AppData): String {
        val session = requireNotNull(data.session)
        require(session.status != SessionStatus.RUNNING) { "Stop timing before exporting." }
        return buildString {
            append("session_id,started_at,athlete_number,athlete_name,event,split_number,total_seconds,interval_seconds\r\n")
            for (athlete in data.athletes) {
                val prefix = listOf(text(session.id), text(session.startedAt), athlete.number.toString(), text(safeName(athlete.name)))
                for (split in session.splitsFor(athlete.number)) {
                    append((prefix + listOf("split", split.number.toString(), formatSeconds(split.totalMs), formatSeconds(split.intervalMs))).joinToString(","))
                    append("\r\n")
                }
                session.stoppedAfterMs?.let { duration ->
                    append((prefix + listOf("stop", "", formatSeconds(duration), "")).joinToString(","))
                    append("\r\n")
                }
            }
        }
    }

    private fun text(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

    // CSV quoting alone does not prevent a spreadsheet from executing a name as a formula.
    private fun safeName(value: String): String {
        val formulaPrefix = value.trimStart().firstOrNull() in listOf('=', '+', '-', '@')
        val controlPrefix = value.firstOrNull() in listOf('\t', '\r', '\n')
        return if (formulaPrefix || controlPrefix) "'" + value else value
    }
}
