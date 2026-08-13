package buzz.delena.crier.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import buzz.delena.crier.settings.CrierSettingsStore

/** Re-arms the foreground relay after a reboot, if the user had it enabled. */
class BootRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = CrierSettingsStore(context)
        if (!settings.assistantEnabled) return
        ContextCompat.startForegroundService(context, Intent(context, CrierForegroundService::class.java))
    }
}
