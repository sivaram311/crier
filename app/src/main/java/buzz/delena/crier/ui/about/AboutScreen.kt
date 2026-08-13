package buzz.delena.crier.ui.about

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import buzz.delena.crier.BuildInfo

/** CONSCIOUS rule #24: name + version must be human-visible without dev tools. */
@Composable
fun AboutScreen(onBack: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onBack) {
                Text("← Back")
            }
            Spacer(Modifier.width(12.dp))
            Text("About", style = MaterialTheme.typography.headlineMedium)
        }

        Text(BuildInfo.APP_NAME, style = MaterialTheme.typography.titleLarge)
        Text("Version ${BuildInfo.VERSION_NAME} (build ${BuildInfo.VERSION_CODE})", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Package buzz.delena.crier · sandbox DEV build · public repo sivaram311/crier",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "A standalone Gemini-voice notification relay: reads your notifications aloud, " +
                "waits out phone calls, and runs quietly in the background.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
