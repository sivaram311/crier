package buzz.delena.crier.ui.home

import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import buzz.delena.crier.BuildInfo
import buzz.delena.crier.notify.NotificationAccess
import buzz.delena.crier.notify.RuntimeGrants
import buzz.delena.crier.service.BatteryOptimization
import buzz.delena.crier.service.CrierForegroundService
import buzz.delena.crier.service.CrierStatusBus
import buzz.delena.crier.settings.CrierSettingsStore

private data class StatusRow(val label: String, val value: String, val ok: Boolean)

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenPlayground: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val settings = remember { CrierSettingsStore(context) }
    val status by CrierStatusBus.status.collectAsState()
    var enabled by remember { mutableStateOf(settings.assistantEnabled) }

    // System permission screens (notification access, battery exemption) and the
    // runtime permission dialog below are all external to this composable, so a
    // one-shot `remember` would keep showing stale "Not granted" after the user
    // comes back having granted them. Bumping this on every resume forces the
    // `remember(resumeTick)` reads below to recompute.
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumeTick++
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        resumeTick++
        // READ_PHONE_STATE only takes effect if CallStateGate re-registers with
        // it; a running service registered (and silently failed) before this
        // grant, so restart it rather than leave call-aware queueing dead.
        if (settings.assistantEnabled && results[android.Manifest.permission.READ_PHONE_STATE] == true) {
            runCatching { CrierForegroundService.restart(context) }
        }
    }
    LaunchedEffect(Unit) {
        val missing = RuntimeGrants.missing(context)
        if (missing.isNotEmpty()) permissionLauncher.launch(missing)
    }

    val hasNotificationAccess = remember(resumeTick) { NotificationAccess.hasAccess(context) }
    val ignoringBatteryOpt = remember(resumeTick) { BatteryOptimization.isIgnoringOptimizations(context) }
    val hasPhoneState = remember(resumeTick) { RuntimeGrants.hasReadPhoneState(context) }
    val hasPostNotifications = remember(resumeTick) { RuntimeGrants.hasPostNotifications(context) }

    val rows = buildList {
        add(StatusRow("Notification listener", if (status.listenerConnected) "Connected" else "Not connected", status.listenerConnected))
        add(StatusRow("Background relay", if (status.foregroundServiceRunning) "Running" else "Stopped", status.foregroundServiceRunning))
        status.lastError?.let { err ->
            add(StatusRow("Last error", err, false))
        }
        val allowedEmpty = settings.allowedPackages().isEmpty()
        add(StatusRow("Allowed apps", if (allowedEmpty) "None (tap Settings)" else "${settings.allowedPackages().size} apps allowed", !allowedEmpty))
        add(StatusRow("Call state", if (status.callActive) "On a call — queueing" else "Idle", !status.callActive))
        add(StatusRow("Queued notifications", status.queuedCount.toString(), status.queuedCount == 0))
        add(StatusRow("Notification access", if (hasNotificationAccess) "Granted" else "Not granted", hasNotificationAccess))
        add(StatusRow("Phone-state permission", if (hasPhoneState) "Granted" else "Not granted — calls won't be detected", hasPhoneState))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(StatusRow("Notification permission", if (hasPostNotifications) "Granted" else "Not granted", hasPostNotifications))
        }
        add(StatusRow("Battery optimization", if (ignoringBatteryOpt) "Exempted" else "Not exempted", ignoringBatteryOpt))
    }


    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(BuildInfo.APP_NAME, style = MaterialTheme.typography.headlineMedium)
            Text(
                "Reads your notifications aloud with Gemini voices — quietly, in the background.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Relay enabled", style = MaterialTheme.typography.titleMedium)
                        Text("Speak notifications as they arrive", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            settings.assistantEnabled = it
                            val serviceIntent = Intent(context, CrierForegroundService::class.java)
                            if (it) {
                                ContextCompat.startForegroundService(context, serviceIntent)
                            } else {
                                context.stopService(serviceIntent)
                            }
                        },
                    )
                }
            }
        }

        if (settings.allowedPackages().isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "No apps allowed to speak",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Go to Settings and check which apps are allowed to speak their notifications, otherwise the app will remain silent.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }


        items(rows) { row ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(row.label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        row.value,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (row.ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        if (!hasNotificationAccess) {
            item {
                Button(onClick = { context.startActivity(NotificationAccess.settingsIntent(context)) }) {
                    Text("Grant notification access")
                }
            }
        }

        if (!hasPhoneState || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotifications)) {
            item {
                Button(onClick = { permissionLauncher.launch(RuntimeGrants.missing(context)) }) {
                    Text("Grant phone/notification permissions")
                }
            }
        }

        if (!ignoringBatteryOpt) {
            item {
                Button(onClick = { context.startActivity(BatteryOptimization.requestExemptionIntent(context)) }) {
                    Text("Exempt from battery optimization")
                }
            }
        }

        item { Spacer(Modifier.height(4.dp)) }

        item {
            Button(onClick = onOpenPlayground, modifier = Modifier.fillMaxWidth()) { Text("Open Playground") }
        }
        item {
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
        }
        item {
            Button(onClick = onOpenAbout, modifier = Modifier.fillMaxWidth()) { Text("About") }
        }
    }
}
