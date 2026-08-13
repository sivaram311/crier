package buzz.delena.crier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import buzz.delena.crier.ui.CrierNavHost
import buzz.delena.crier.ui.theme.CrierTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CrierRoot() }
    }
}

@Composable
private fun CrierRoot() {
    CrierTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CrierNavHost()
        }
    }
}
