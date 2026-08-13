package buzz.delena.crier.ui.logs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import buzz.delena.crier.log.CrierLogBus
import buzz.delena.crier.log.LogEntry
import buzz.delena.crier.log.LogLevel
import buzz.delena.crier.ui.theme.CrierMint
import buzz.delena.crier.ui.theme.CrierRed
import buzz.delena.crier.ui.theme.CrierViolet

@Composable
fun LogsScreen(onBack: () -> Unit = {}) {
    val logs by CrierLogBus.logs.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedLevel by remember { mutableStateOf<LogLevel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filteredLogs = remember(logs, selectedLevel, searchQuery) {
        logs.filter { entry ->
            (selectedLevel == null || entry.level == selectedLevel) &&
                (searchQuery.isBlank() || entry.message.contains(searchQuery, ignoreCase = true) ||
                    entry.tag.contains(searchQuery, ignoreCase = true) ||
                    (entry.payload?.contains(searchQuery, ignoreCase = true) == true) ||
                    (entry.response?.contains(searchQuery, ignoreCase = true) == true))
        }
    }

    LaunchedEffect(logs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
                Text("Live Logs", style = MaterialTheme.typography.headlineMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val allText = logs.joinToString("\n") {
                        "[${it.formattedTime}] [${it.level}] [${it.tag}] ${it.message}" +
                            (it.payload?.let { p -> "\n  Payload: $p" } ?: "") +
                            (it.response?.let { r -> "\n  Response: $r" } ?: "")
                    }
                    clipboardManager.setText(AnnotatedString(allText))
                    Toast.makeText(context, "Copied ${logs.size} logs", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Copy")
                }
                OutlinedButton(onClick = { CrierLogBus.clear() }) {
                    Text("Clear")
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                selected = selectedLevel == null,
                onClick = { selectedLevel = null },
                label = { Text("ALL (${logs.size})") },
            )
            LogLevel.entries.forEach { lvl ->
                val count = logs.count { it.level == lvl }
                FilterChip(
                    selected = selectedLevel == lvl,
                    onClick = { selectedLevel = if (selectedLevel == lvl) null else lvl },
                    label = { Text("$lvl ($count)") },
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Filter logs...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        if (filteredLogs.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (logs.isEmpty()) "No logs captured yet. Trigger actions or notifications to see live debug events." else "No matching logs found.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredLogs, key = { it.id }) { entry ->
                    LogCard(entry)
                }
            }
        }
    }
}

@Composable
private fun LogCard(entry: LogEntry) {
    var expanded by remember { mutableStateOf(false) }

    val levelColor = when (entry.level) {
        LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
        LogLevel.INFO -> CrierMint
        LogLevel.WARN -> MaterialTheme.colorScheme.secondary
        LogLevel.ERROR -> CrierRed
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(levelColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            entry.level.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = levelColor,
                        )
                    }
                    Text(entry.tag, style = MaterialTheme.typography.labelMedium, color = CrierViolet)
                }
                Text(entry.formattedTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(entry.message, style = MaterialTheme.typography.bodyMedium)

            if (entry.payload != null || entry.response != null) {
                Text(
                    if (expanded) "Hide details ▲" else "Show API payload / response details ▼",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (expanded) {
                entry.payload?.let { payload ->
                    Text("Request Payload:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(8.dp),
                    ) {
                        Text(
                            payload,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        )
                    }
                }

                entry.response?.let { response ->
                    Spacer(Modifier.height(4.dp))
                    Text("Response / Output:", style = MaterialTheme.typography.labelSmall, color = CrierMint)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(8.dp),
                    ) {
                        Text(
                            response,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        )
                    }
                }
            }
        }
    }
}
