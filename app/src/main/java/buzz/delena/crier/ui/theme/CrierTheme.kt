package buzz.delena.crier.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CrierDarkScheme = darkColorScheme(
    primary = CrierViolet,
    onPrimary = CrierInk50,
    secondary = CrierMint,
    background = CrierInk,
    surface = CrierSurface,
    surfaceVariant = CrierSurfaceHigh,
    onBackground = CrierInk50,
    onSurface = CrierInk50,
    error = CrierRed,
)

private val CrierLightScheme = lightColorScheme(
    primary = CrierViolet,
    secondary = CrierMint,
)

/** Dark-first "studio playground" look — light scheme kept only as a system fallback. */
@Composable
fun CrierTheme(
    useDark: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = if (useDark) CrierDarkScheme else CrierLightScheme
    MaterialTheme(
        colorScheme = colors,
        typography = CrierTypography,
        content = content,
    )
}
