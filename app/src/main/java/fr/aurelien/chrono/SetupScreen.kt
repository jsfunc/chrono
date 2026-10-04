package fr.aurelien.chrono

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
internal fun SetupScreen(athletes: List<Athlete>, onReady: (List<String>) -> Unit) {
    var count by rememberSaveable { mutableIntStateOf(athletes.size) }
    var names by rememberSaveable {
        mutableStateOf((1..16).map { number -> athletes.find { it.number == number }?.name ?: "Athlete $number" })
    }
    val focus = LocalFocusManager.current
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Chrono", style = MaterialTheme.typography.headlineLarge)
        Text("One start. Every athlete's splits.", color = ChronoColors.Muted, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Athletes", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { count-- }, enabled = count > 1, modifier = Modifier.testTag("decrease-count"), contentPadding = PaddingValues(0.dp)) { Text("−") }
                Text(count.toString(), Modifier.width(40.dp).wrapContentWidth(Alignment.CenterHorizontally).testTag("athlete-count"), style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = { count++ }, enabled = count < 16, modifier = Modifier.testTag("increase-count"), contentPadding = PaddingValues(0.dp)) { Text("+") }
            }
        }
        Text("Name each zone", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items((0 until count).toList(), key = { it }) { index ->
                OutlinedTextField(
                    value = names[index],
                    onValueChange = { newName -> names = names.toMutableList().also { it[index] = newName } },
                    label = { Text("Athlete ${index + 1}") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().testTag("name-${index + 1}"),
                )
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
        Button(
            onClick = { focus.clearFocus(); onReady(names.take(count)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).heightIn(min = 56.dp).testTag("ready"),
            shape = MaterialTheme.shapes.large,
        ) { Text("Ready to time", style = MaterialTheme.typography.titleMedium) }
    }
}
