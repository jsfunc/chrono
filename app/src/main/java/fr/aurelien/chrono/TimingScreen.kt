package fr.aurelien.chrono

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

@Composable
internal fun TimingScreen(
    data: AppData,
    exporting: Boolean,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onTap: (Int) -> Unit,
    onResults: () -> Unit,
    onExport: () -> Unit,
    onNewSession: () -> Unit,
) {
    val session = data.session
    val running = session?.status == SessionStatus.RUNNING
    val ended = session != null && !running
    var zonesFit by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Chrono", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (session == null) TextButton(onClick = onEdit) { Text("Edit athletes") }
            else Text("${data.athletes.size} athletes", color = ChronoColors.Muted, style = MaterialTheme.typography.bodySmall)
        }
        SharedTimer(session)
        if (session?.status == SessionStatus.INTERRUPTED) {
            Text("Timing was interrupted. Recorded splits are preserved.", color = ChronoColors.Red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val columns = if (data.athletes.size <= 4) 1 else 2
            val rows = (data.athletes.size + columns - 1) / columns
            val cellHeight = (maxHeight - 6.dp * (rows - 1)) / rows
            val dense = cellHeight < 96.dp
            val fontScale = LocalDensity.current.fontScale
            val tooSmall = cellHeight < (48 * fontScale + 8).dp
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(rows) { row ->
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(columns) { column ->
                            val athlete = data.athletes.getOrNull(row * columns + column)
                            if (athlete == null) Spacer(Modifier.weight(1f))
                            else AthleteZone(
                                athlete = athlete,
                                splits = session?.splitsFor(athlete.number).orEmpty(),
                                dense = dense,
                                enabled = running || ended,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = { if (running) onTap(athlete.number) else if (ended) onResults() },
                            )
                        }
                    }
                }
            }
            if (tooSmall && session == null) {
                // A phone cannot fit unlimited text enlargement and 16 targets at once.
                Surface(Modifier.align(Alignment.Center).padding(12.dp), color = Color.White, shadowElevation = 6.dp, shape = MaterialTheme.shapes.medium) {
                    Text("These zones need more room. Choose fewer athletes or a smaller text size.", Modifier.padding(16.dp), color = ChronoColors.Red)
                }
            }
            SideEffect { zonesFit = !tooSmall }
        }
        if (ended) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onResults, modifier = Modifier.weight(1f)) { Text("Results") }
                OutlinedButton(onClick = onExport, enabled = !exporting, modifier = Modifier.weight(1f)) { Text(if (exporting) "Saving…" else "Export CSV") }
            }
        }
        Button(
            onClick = { when { running -> onStop(); ended -> onNewSession(); else -> onStart() } },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp).heightIn(min = 56.dp).testTag("shared-button"),
            colors = ButtonDefaults.buttonColors(containerColor = if (running) ChronoColors.Red else ChronoColors.Green),
            shape = MaterialTheme.shapes.large,
            enabled = zonesFit || running || ended,
        ) {
            Text(when { running -> "Stop"; ended -> "New session"; else -> "Start" }, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun SharedTimer(session: Session?) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var elapsed by remember(session?.id, session?.status) {
        mutableLongStateOf(when (session?.status) {
            SessionStatus.RUNNING -> (SystemClock.elapsedRealtime() - session.startElapsedMs).coerceAtLeast(0)
            SessionStatus.STOPPED -> session.stoppedAfterMs ?: 0
            SessionStatus.INTERRUPTED -> session.taps.lastOrNull()?.elapsedMs ?: 0
            null -> 0
        })
    }
    LaunchedEffect(session?.id, session?.status, lifecycle) {
        if (session?.status == SessionStatus.RUNNING) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    elapsed = (SystemClock.elapsedRealtime() - session.startElapsedMs).coerceAtLeast(0)
                    delay(100)
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        val state = when (session?.status) {
            SessionStatus.RUNNING -> "Running"
            SessionStatus.STOPPED -> "Stopped"
            SessionStatus.INTERRUPTED -> "Last recorded"
            null -> "Ready"
        }
        Text(state, Modifier.weight(1f), color = if (session?.status == SessionStatus.RUNNING) ChronoColors.Green else ChronoColors.Muted)
        Text(formatDuration(elapsed), fontFamily = FontFamily.Monospace, fontSize = 28.sp, fontWeight = FontWeight.Medium, modifier = Modifier.testTag("shared-time"))
    }
}

@Composable
internal fun AthleteZone(athlete: Athlete, splits: List<Split>, dense: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    var highlighted by remember { mutableStateOf(false) }
    LaunchedEffect(splits.size) {
        if (splits.isNotEmpty()) { highlighted = true; delay(220); highlighted = false }
    }
    val background by animateColorAsState(if (highlighted) ChronoColors.PaleGreen else Color.White, label = "split-feedback")
    val latest = splits.lastOrNull()
    Surface(
        modifier = modifier.testTag("athlete-${athlete.number}").clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        color = background,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, ChronoColors.Line),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val historyStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 16.sp)
            val textMeasurer = rememberTextMeasurer()
            // Reserve the existing name/latest-time block before using spare height for history.
            val latestHeight = with(density) { (if (dense) 48.sp else 65.sp).toDp() } + if (dense) 8.dp else 28.dp
            val historyRowHeight = with(density) { 16.sp.toDp() }
            val historyHeaderHeight = historyRowHeight + 11.dp
            val historySlots = ((maxHeight - latestHeight - historyHeaderHeight - 4.dp) / historyRowHeight)
                .toInt().coerceIn(0, 3)
            val recent = splits.dropLast(1).takeLast(historySlots).asReversed()
            val numberWidth = with(density) {
                recent.maxOfOrNull { textMeasurer.measure(AnnotatedString(it.number.toString()), historyStyle).size.width }
                    ?.toDp() ?: 16.dp
            }.coerceAtLeast(16.dp)
            val timeWidth = with(density) {
                maxOf(
                    textMeasurer.measure(AnnotatedString("Interval"), historyStyle).size.width,
                    recent.maxOfOrNull {
                        textMeasurer.measure(AnnotatedString(formatDuration(it.totalMs)), historyStyle).size.width
                    } ?: 0,
                ).toDp()
            }
            // Measuring the actual digits also handles long sessions and narrow phone screens.
            val historyFitsWidth = numberWidth + timeWidth * 2 + 12.dp <= maxWidth - 18.dp
            Column(Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = if (dense) 4.dp else 10.dp), verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${athlete.number}. ${athlete.name}", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = if (dense) 13.sp else 17.sp, lineHeight = if (dense) 16.sp else 21.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("#${splits.size}", fontSize = 11.sp, lineHeight = 14.sp, color = ChronoColors.Muted, modifier = Modifier.padding(start = 4.dp))
                }
                if (!dense) Spacer(Modifier.height(8.dp))
                TimeValue("Total", latest?.totalMs, dense, Modifier.testTag("total-${athlete.number}"))
                TimeValue("Interval", latest?.intervalMs, dense, Modifier.testTag("interval-${athlete.number}"))
                if (recent.isNotEmpty() && historyFitsWidth) {
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = ChronoColors.Line, thickness = 1.dp)
                    Spacer(Modifier.height(4.dp))
                    RecentSplitRow("#", "Total", "Interval", numberWidth, historyStyle, Modifier)
                    for (split in recent) {
                        RecentSplitRow(
                            split.number.toString(), formatDuration(split.totalMs), formatDuration(split.intervalMs),
                            numberWidth, historyStyle,
                            Modifier.testTag("recent-${athlete.number}-${split.number}"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeValue(label: String, value: Long?, dense: Boolean, modifier: Modifier) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = ChronoColors.Muted, fontSize = if (dense) 11.sp else 12.sp, lineHeight = if (dense) 15.sp else 22.sp)
        Text(value?.let(::formatDuration) ?: "—", modifier, fontFamily = FontFamily.Monospace, fontSize = if (dense) 13.sp else 18.sp, lineHeight = if (dense) 16.sp else 22.sp, maxLines = 1)
    }
}

@Composable
private fun RecentSplitRow(number: String, total: String, interval: String, numberWidth: Dp, style: TextStyle, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(number, Modifier.width(numberWidth), style = style, color = ChronoColors.Muted, maxLines = 1)
        Text(total, Modifier.weight(1f), style = style, color = ChronoColors.Muted, textAlign = TextAlign.End, maxLines = 1)
        Text(interval, Modifier.weight(1f), style = style, color = ChronoColors.Muted, textAlign = TextAlign.End, maxLines = 1)
    }
}
