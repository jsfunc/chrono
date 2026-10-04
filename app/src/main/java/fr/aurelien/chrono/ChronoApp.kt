package fr.aurelien.chrono

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

internal object ChronoColors {
    val Background = Color(0xFFF4F6F8)
    val Ink = Color(0xFF15263E)
    val Muted = Color(0xFF526174)
    val Green = Color(0xFF007A67)
    val PaleGreen = Color(0xFFDCF3EB)
    val Red = Color(0xFFB13636)
    val Line = Color(0xFFDCE2E8)
}

@Composable
fun ChronoApp(viewModel: ChronoViewModel) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = ChronoColors.Green,
        onPrimary = Color.White,
        background = ChronoColors.Background,
        surface = Color.White,
        onSurface = ChronoColors.Ink,
        onBackground = ChronoColors.Ink,
        error = ChronoColors.Red,
    )) {
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val running = viewModel.data.session?.status == SessionStatus.RUNNING
        val view = LocalView.current
        DisposableEffect(view, running) {
            // Android automatically permits sleep when this window is in the background.
            view.keepScreenOn = running
            onDispose { view.keepScreenOn = false }
        }
        LaunchedEffect(viewModel.notice) {
            viewModel.notice?.let { message ->
                snackbar.showSnackbar(message)
                viewModel.clearNotice()
            }
        }
        Surface(Modifier.fillMaxSize(), color = ChronoColors.Background) {
            Scaffold(
                containerColor = ChronoColors.Background,
                snackbarHost = { SnackbarHost(snackbar) },
                contentWindowInsets = WindowInsets.safeDrawing,
            ) { insets ->
                Box(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
                    when {
                        viewModel.loadError -> Column(
                            Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Could not open the saved session.", style = MaterialTheme.typography.titleMedium)
                            Text("Your saved file has been kept. Please try again.", modifier = Modifier.padding(vertical = 12.dp))
                            Button(onClick = viewModel::load) { Text("Retry") }
                        }
                        !viewModel.loaded -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        else -> {
                            var screen by rememberSaveable {
                                mutableStateOf(if (viewModel.data.session == null) "setup" else "timing")
                            }
                            var confirmNew by remember { mutableStateOf(false) }
                            val data = viewModel.data
                            val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
                                if (uri != null) viewModel.export(uri)
                            }
                            val export = { exporter.launch(viewModel.exportFilename()) }
                            BackHandler(enabled = running || screen == "results" || (screen == "timing" && data.session == null)) {
                                when {
                                    running -> scope.launch { snackbar.showSnackbar("Timing is running. Use Stop to finish.") }
                                    screen == "results" -> screen = "timing"
                                    else -> screen = "setup"
                                }
                            }
                            Column(Modifier.fillMaxSize()) {
                                if (viewModel.saveError) {
                                    Surface(color = Color(0xFFFFE9E5)) {
                                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("Results could not be saved. Export them before closing.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                            TextButton(onClick = viewModel::retrySave) { Text("Retry") }
                                        }
                                    }
                                }
                                Box(Modifier.weight(1f)) {
                                    when (screen) {
                                        "setup" -> SetupScreen(data.athletes) { names ->
                                            viewModel.configure(names)
                                            screen = "timing"
                                        }
                                        "results" -> ResultsScreen(data, viewModel.exporting, export) { screen = "timing" }
                                        else -> TimingScreen(
                                            data = data,
                                            exporting = viewModel.exporting,
                                            onEdit = { screen = "setup" },
                                            onStart = viewModel::start,
                                            onStop = viewModel::stop,
                                            onTap = viewModel::record,
                                            onResults = { screen = "results" },
                                            onExport = export,
                                            onNewSession = { confirmNew = true },
                                        )
                                    }
                                }
                            }
                            if (confirmNew) {
                                AlertDialog(
                                    onDismissRequest = { confirmNew = false },
                                    title = { Text("Start a new session?") },
                                    text = { Text("This clears the current results and keeps the athlete names. Export your CSV first if you want to keep a copy.") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            viewModel.newSession()
                                            confirmNew = false
                                            screen = "setup"
                                        }) { Text("Clear results") }
                                    },
                                    dismissButton = { TextButton(onClick = { confirmNew = false }) { Text("Keep results") } },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
