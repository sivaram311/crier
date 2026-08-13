package buzz.delena.crier.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import buzz.delena.crier.gemini.GeminiModelCatalog
import buzz.delena.crier.gemini.GeminiModelOption
import buzz.delena.crier.settings.CrierSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onOpenLogs: () -> Unit = {},
) {
    val context = LocalContext.current
    val settings = remember { CrierSettingsStore(context) }
    val scope = rememberCoroutineScope()

    var storedKey by remember { mutableStateOf(settings.apiKey().orEmpty()) }
    var apiKeySaved by remember { mutableStateOf(settings.hasApiKey) }
    var isEditingKey by remember { mutableStateOf(!settings.hasApiKey) }
    var keyInput by remember { mutableStateOf(if (isEditingKey) "" else storedKey) }

    var availableModels by remember { mutableStateOf<List<GeminiModelOption>>(GeminiModelCatalog.TTS_MODELS) }
    var isFetchingModels by remember { mutableStateOf(false) }

    var model by remember { mutableStateOf(settings.ttsModel) }
    var voice by remember { mutableStateOf(settings.voiceName) }
    var language by remember { mutableStateOf(settings.languageCode) }
    var systemPrompt by remember { mutableStateOf(settings.systemPrompt) }
    var speakWhenLocked by remember { mutableStateOf(settings.speakWhenLocked) }
    var allowAllApps by remember { mutableStateOf(settings.allowAllApps) }
    var searchQuery by remember { mutableStateOf("") }

    fun refreshModels(key: String) {
        if (key.isBlank()) return
        isFetchingModels = true
        scope.launch(Dispatchers.IO) {
            val list = GeminiModelCatalog.fetchAvailableModels(key)
            withContext(Dispatchers.Main) {
                availableModels = list
                isFetchingModels = false
            }
        }
    }

    LaunchedEffect(apiKeySaved) {
        if (apiKeySaved && storedKey.isNotBlank()) {
            refreshModels(storedKey)
        }
    }

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

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.second.contains(searchQuery, ignoreCase = true) ||
                    it.first.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onBack) {
                        Text("← Back")
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Settings", style = MaterialTheme.typography.headlineMedium)
                }
                OutlinedButton(onClick = onOpenLogs) {
                    Text("Live Logs")
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Gemini API key", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (apiKeySaved) "Saved (read-only, on-device encrypted)." else "Enter your API key below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (apiKeySaved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    )

                    OutlinedTextField(
                        value = if (isEditingKey) keyInput else storedKey,
                        onValueChange = { if (isEditingKey) keyInput = it },
                        readOnly = !isEditingKey,
                        label = { Text(if (!isEditingKey) "API Key (Active & Read-only)" else "Paste API Key") },
                        placeholder = { Text("AIzaSy...") },
                        visualTransformation = VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isEditingKey) {
                            Button(onClick = {
                                val clean = keyInput.trim()
                                if (clean.isNotEmpty() && settings.saveApiKey(clean)) {
                                    storedKey = clean
                                    apiKeySaved = true
                                    isEditingKey = false
                                    refreshModels(clean)
                                    Toast.makeText(context, "API Key saved successfully", Toast.LENGTH_SHORT).show()
                                }
                            }) { Text("Save key") }

                            if (apiKeySaved) {
                                OutlinedButton(onClick = {
                                    isEditingKey = false
                                    keyInput = storedKey
                                }) { Text("Cancel") }
                            }
                        } else {
                            Button(onClick = {
                                keyInput = storedKey
                                isEditingKey = true
                            }) { Text("Edit Key") }

                            OutlinedButton(onClick = {
                                settings.saveApiKey("")
                                storedKey = ""
                                keyInput = ""
                                apiKeySaved = false
                                isEditingKey = true
                                Toast.makeText(context, "API Key cleared", Toast.LENGTH_SHORT).show()
                            }) { Text("Clear key") }
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Voice & Models", style = MaterialTheme.typography.titleMedium)
                        if (apiKeySaved) {
                            OutlinedButton(
                                onClick = { refreshModels(storedKey) },
                                enabled = !isFetchingModels,
                            ) {
                                Text(if (isFetchingModels) "Fetching..." else "Refresh Models")
                            }
                        }
                    }

                    Text(
                        "${availableModels.size} models available for this API key",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )

                    LabeledDropdown(
                        label = "Model",
                        options = availableModels.map { it.id to it.label },
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("System Prompt / Persona", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Instructs Gemini on how to interpret, summarize, and phrase notification readings.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = {
                            systemPrompt = it
                            settings.systemPrompt = it
                        },
                        label = { Text("System Instruction") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            systemPrompt = CrierSettingsStore.DEFAULT_SYSTEM_PROMPT
                            settings.systemPrompt = systemPrompt
                        }) {
                            Text("Reset to Default")
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Speak when screen is locked", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Read notifications aloud when device is locked/in pocket",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Switch(
                            checked = speakWhenLocked,
                            onCheckedChange = {
                                speakWhenLocked = it
                                settings.speakWhenLocked = it
                            },
                        )
                    }
                }
            }
        }

        item {
            var quietEnabled by remember { mutableStateOf(settings.quietHoursEnabled) }
            var quietStartMinutes by remember { mutableIntStateOf(settings.quietStartMinutes) }
            var quietEndMinutes by remember { mutableIntStateOf(settings.quietEndMinutes) }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Quiet Hours", style = MaterialTheme.typography.titleMedium)
                            Text("Mute notification speech during these hours", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = quietEnabled,
                            onCheckedChange = {
                                quietEnabled = it
                                settings.quietHoursEnabled = it
                            },
                        )
                    }
                    if (quietEnabled) {
                        val hoursOptions = remember { (0..23).map { String.format("%02d", it) to String.format("%02d", it) } }
                        val minutesOptions = remember { (0..59).map { String.format("%02d", it) to String.format("%02d", it) } }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            LabeledDropdown(
                                label = "Start Hour",
                                options = hoursOptions,
                                selected = String.format("%02d", quietStartMinutes / 60),
                                onSelected = { h ->
                                    quietStartMinutes = h.toInt() * 60 + (quietStartMinutes % 60)
                                    settings.quietStartMinutes = quietStartMinutes
                                },
                            )
                            LabeledDropdown(
                                label = "Start Minute",
                                options = minutesOptions,
                                selected = String.format("%02d", quietStartMinutes % 60),
                                onSelected = { m ->
                                    quietStartMinutes = (quietStartMinutes / 60) * 60 + m.toInt()
                                    settings.quietStartMinutes = quietStartMinutes
                                },
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            LabeledDropdown(
                                label = "End Hour",
                                options = hoursOptions,
                                selected = String.format("%02d", quietEndMinutes / 60),
                                onSelected = { h ->
                                    quietEndMinutes = h.toInt() * 60 + (quietEndMinutes % 60)
                                    settings.quietEndMinutes = quietEndMinutes
                                },
                            )
                            LabeledDropdown(
                                label = "End Minute",
                                options = minutesOptions,
                                selected = String.format("%02d", quietEndMinutes % 60),
                                onSelected = { m ->
                                    quietEndMinutes = (quietEndMinutes / 60) * 60 + m.toInt()
                                    settings.quietEndMinutes = quietEndMinutes
                                },
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Allow all apps", style = MaterialTheme.typography.titleMedium)
                            Text("Speak notifications from every installed app", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = allowAllApps,
                            onCheckedChange = {
                                allowAllApps = it
                                settings.allowAllApps = it
                            },
                        )
                    }

                    if (!allowAllApps) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val allPkgs = installedApps.map { it.first }
                                    settings.setAllPackagesAllowed(allPkgs)
                                    allowed = settings.allowedPackages()
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Select All")
                            }
                            OutlinedButton(
                                onClick = {
                                    settings.clearAllAllowedPackages()
                                    allowed = emptySet()
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Clear All")
                            }
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search installed apps") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }
            }
        }

        if (!allowAllApps) {
            item {
                Text(
                    "App Allowlist (${allowed.size} of ${installedApps.size} enabled)",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            items(filteredApps, key = { it.first }) { (pkg, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        Text(pkg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
