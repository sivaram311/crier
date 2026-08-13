package buzz.delena.crier.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * The foreground-service notification alone doesn't stop OEM battery
 * managers (Realme's stock "Sleep standby optimization" among them) from
 * killing background work. Requesting the ignore-optimizations exemption
 * is the documented mitigation; the OEM autostart allowlist still needs a
 * manual nudge in Settings, which this can't reach programmatically.
 */
object BatteryOptimization {
    fun isIgnoringOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    @SuppressLint("BatteryLife")
    fun requestExemptionIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
}
