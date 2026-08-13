package buzz.delena.crier.ui.playground

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import buzz.delena.crier.gemini.GeminiAudioPlayer
import buzz.delena.crier.gemini.GeminiAudioResult
import buzz.delena.crier.gemini.GeminiCapability
import buzz.delena.crier.gemini.GeminiModelCatalog
import buzz.delena.crier.gemini.GeminiModelOption
import buzz.delena.crier.gemini.GeminiTtsClient
import buzz.delena.crier.settings.CrierSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlaygroundScreen(
    onBack: () -> Unit = {},
    onOpenLogs: () -> Unit = {},
) {
    val context = LocalContext.current
    val settings = remember { CrierSettingsStore(context) }
    val scope = rememberCoroutineScope()

    var availableModels by remember { mutableStateOf<List<GeminiModelOption>>(GeminiModelCatalog.TTS_MODELS) }
    var selectedModel by remember {
        val saved = settings.ttsModel
        val option = availableModels.firstOrNull { it.id == saved }
            ?: availableModels.first()
        mutableStateOf(option)
    }
    var voice by remember { mutableStateOf(settings.voiceName) }
    var language by remember { mutableStateOf(settings.languageCode) }
    var systemPrompt by remember { mutableStateOf(settings.systemPrompt) }
    var prompt by remember { mutableStateOf("Hey, this is Crier checking in. Your notifications are running smoothly!") }
    var status by remember { mutableStateOf<String?>(null) }
    var isSynthesizing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val apiKey = settings.apiKey()
        if (!apiKey.isNullOrBlank()) {
            scope.launch(Dispatchers.IO) {
                val list = GeminiModelCatalog.fetchAvailableModels(apiKey)
                withContext(Dispatchers.Main) {
                    availableModels = list
                    if (availableModels.none { it.id == selectedModel.id }) {
                        selectedModel = availableModels.firstOrNull() ?: selectedModel
                    }
                }
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
                    Text("Playground", style = MaterialTheme.typography.headlineMedium)
                }
                OutlinedButton(onClick = onOpenLogs) {
                    Text("Live Logs")
                }
            }
        }

        item {
            Text(
                "Try every Gemini voice model available for your API key. Inspect API payloads and responses in Live Logs.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        item {
            Text("Available TTS Models (${availableModels.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(availableModels) { option ->
            ModelRow(option, selected = selectedModel.id == option.id) {
                selectedModel = option
                settings.ttsModel = option.id
            }
        }

        item { Text("Speech-to-text (STT)", style = MaterialTheme.typography.titleMedium) }
        items(GeminiModelCatalog.STT_MODELS) { option ->
            ModelRow(option, selected = false, onSelect = {})
        }

        item { Text("Live API (real-time voice)", style = MaterialTheme.typography.titleMedium) }
        items(GeminiModelCatalog.LIVE_MODELS) { option ->
            ModelRow(option, selected = false, onSelect = {})
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Test Speak with System Prompt", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = {
                            systemPrompt = it
                            settings.systemPrompt = it
                        },
                        label = { Text("System Prompt / Persona") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("Notification text to speak") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )

                    Button(
                        onClick = {
                            val apiKey = settings.apiKey()
                            if (apiKey.isNullOrBlank()) {
                                status = "Add a Gemini API key in Settings first."
                                return@Button
                            }
                            status = "Synthesizing with ${selectedModel.label}…"
                            isSynthesizing = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val client = GeminiTtsClient()
                                    val result = client.synthesize(
                                        apiKey = apiKey,
                                        model = selectedModel.id,
                                        prompt = prompt,
                                        voice = voice,
                                        languageCode = language,
                                        systemPrompt = systemPrompt,
                                    )
                                    client.close()
                                    status = when (result) {
                                        is GeminiAudioResult.Success -> {
                                            GeminiAudioPlayer().play(result.pcm, result.sampleRateHz)
                                            "Played (${result.pcm.size} bytes @ ${result.sampleRateHz}Hz)."
                                        }
                                        GeminiAudioResult.Unauthorized -> "API key rejected."
                                        GeminiAudioResult.ModelUnavailable -> "Model unavailable for this key."
                                        GeminiAudioResult.Timeout -> "Request timed out."
                                        GeminiAudioResult.Malformed -> "Malformed response."
                                        GeminiAudioResult.Unavailable -> "Service unavailable — check Live Logs."
                                    }
                                } catch (e: Exception) {
                                    status = "Error: ${e.message}"
                                } finally {
                                    isSynthesizing = false
                                }
                            }
                        },
                        enabled = !isSynthesizing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (isSynthesizing) "Synthesizing…" else "Speak")
                    }
                    status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
}

@Composable
private fun ModelRow(option: GeminiModelOption, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (option.capability == GeminiCapability.TTS) {
                RadioButton(selected = selected, onClick = onSelect)
            }
            Text(option.label, style = MaterialTheme.typography.bodyLarge)
        }
        if (!option.wiredInThisBuild) {
            AssistChip(onClick = {}, label = { Text("v0.2.0") })
        }
    }
}
