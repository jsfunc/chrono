package fr.aurelien.chrono

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun ResultsScreen(data: AppData, exporting: Boolean, onExport: () -> Unit, onBack: () -> Unit) {
    val session = data.session ?: return
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Results", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 10.dp))
            TextButton(onClick = onBack) { Text("Back") }
        }
        Text(OffsetDateTime.parse(session.startedAt).format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.ENGLISH)), color = ChronoColors.Muted)
        Text(
            session.stoppedAfterMs?.let { "Session duration: ${formatDuration(it)}" } ?: "Interrupted session · saved splits only",
            color = if (session.status == SessionStatus.INTERRUPTED) ChronoColors.Red else ChronoColors.Muted,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            data.athletes.forEach { athlete ->
                item(key = "athlete-${athlete.number}") {
                    Surface(shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("${athlete.number}. ${athlete.name}", style = MaterialTheme.typography.titleMedium)
                            val splits = session.splitsFor(athlete.number)
                            if (splits.isEmpty()) Text("No splits recorded.", color = ChronoColors.Muted, modifier = Modifier.padding(top = 8.dp))
                            else {
                                ResultRow("#", "Total", "Interval", heading = true)
                                splits.forEach { split -> ResultRow(split.number.toString(), formatDuration(split.totalMs), formatDuration(split.intervalMs)) }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
        Button(onClick = onExport, enabled = !exporting, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).heightIn(min = 56.dp), shape = MaterialTheme.shapes.large) {
            Text(if (exporting) "Saving…" else "Export CSV", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ResultRow(number: String, total: String, interval: String, heading: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(number, Modifier.weight(0.4f), color = ChronoColors.Muted)
        Text(total, Modifier.weight(1f), fontFamily = if (heading) FontFamily.Default else FontFamily.Monospace, fontWeight = if (heading) FontWeight.Medium else FontWeight.Normal)
        Text(interval, Modifier.weight(1f), fontFamily = if (heading) FontFamily.Default else FontFamily.Monospace, fontWeight = if (heading) FontWeight.Medium else FontWeight.Normal)
    }
}
