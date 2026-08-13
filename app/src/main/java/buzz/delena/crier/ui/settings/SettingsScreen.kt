package buzz.delena.crier.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import buzz.delena.crier.gemini.GeminiModelCatalog
import buzz.delena.crier.settings.CrierSettingsStore

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settings = remember { CrierSettingsStore(context) }

    var apiKey by remember { mutableStateOf("") }
    var apiKeySaved by remember { mutableStateOf(settings.hasApiKey) }
    var model by remember { mutableStateOf(settings.ttsModel) }
    var voice by remember { mutableStateOf(settings.voiceName) }
    var language by remember { mutableStateOf(settings.languageCode) }

    val installedApps = remember {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .filter { it.first != context.packageName }
            .sortedBy { it.second.lowercase() }
    }
    var allowed by remember { mutableStateOf(settings.allowedPackages()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium) }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Gemini API key", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (apiKeySaved) "Saved (encrypted, on-device only)." else "Not set yet.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Paste API key") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = {
                        if (settings.saveApiKey(apiKey)) {
                            apiKeySaved = settings.hasApiKey
                            apiKey = ""
                        }
                    }) { Text("Save key") }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Voice", style = MaterialTheme.typography.titleMedium)
                    LabeledDropdown(
                        label = "Model",
                        options = GeminiModelCatalog.TTS_MODELS.map { it.id to it.label },
                        selected = model,
                        onSelected = { model = it; settings.ttsModel = it },
                    )
                    LabeledDropdown(
                        label = "Voice",
                        options = GeminiModelCatalog.VOICES.map { it to it },
                        selected = voice,
                        onSelected = { voice = it; settings.voiceName = it },
                    )
                    LabeledDropdown(
                        label = "Language",
                        options = GeminiModelCatalog.LANGUAGES,
                        selected = language,
                        onSelected = { language = it; settings.languageCode = it },
                    )
                }
            }
        }

        item {
            Text("Which apps may speak", style = MaterialTheme.typography.titleMedium)
        }

        items(installedApps) { (pkg, label) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
                Switch(
                    checked = pkg in allowed,
                    onCheckedChange = { checked ->
                        settings.setPackageAllowed(pkg, checked)
                        allowed = settings.allowedPackages()
                    },
                )
            }
        }
    }
}

@Composable
private fun LabeledDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.first == selected }?.second ?: selected
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Button(onClick = { expanded = true }) { Text(currentLabel) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, display) ->
                DropdownMenuItem(
                    text = { Text(display) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    },
                )
            }
        }
    }
}
