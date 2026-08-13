package buzz.delena.crier.ui.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun PlaygroundScreen() {
    val context = LocalContext.current
    val settings = remember { CrierSettingsStore(context) }
    val scope = rememberCoroutineScope()

    var selectedModel by remember { mutableStateOf<GeminiModelOption>(GeminiModelCatalog.TTS_MODELS.first()) }
    var voice by remember { mutableStateOf(settings.voiceName) }
    var language by remember { mutableStateOf(settings.languageCode) }
    var prompt by remember { mutableStateOf("Hey, this is Crier checking in.") }
    var status by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Playground", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Try every Gemini voice model this build knows about. TTS runs live; " +
                    "STT and Live API are cataloged for v0.2.0.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        item { Text("Text-to-speech", style = MaterialTheme.typography.titleMedium) }
        items(GeminiModelCatalog.TTS_MODELS) { option ->
            ModelRow(option, selected = selectedModel.id == option.id) { selectedModel = option }
        }

        item { Text("Speech-to-text", style = MaterialTheme.typography.titleMedium) }
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
                    Text("Test speak", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("Text to speak") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            val apiKey = settings.apiKey()
                            if (apiKey.isNullOrBlank()) {
                                status = "Add a Gemini API key in Settings first."
                                return@Button
                            }
                            status = "Synthesizing…"
                            scope.launch(Dispatchers.IO) {
                                val client = GeminiTtsClient()
                                val result = client.synthesize(
                                    apiKey = apiKey,
                                    model = selectedModel.id,
                                    prompt = prompt,
                                    voice = voice,
                                    languageCode = language,
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
                                    GeminiAudioResult.Unavailable -> "Service unavailable — try again."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Speak") }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
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
