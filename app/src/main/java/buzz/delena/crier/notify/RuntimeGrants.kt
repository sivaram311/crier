package buzz.delena.crier.notify

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Dangerous/runtime permissions this app needs beyond the special
 * notification-listener grant (which has its own Settings-screen flow via
 * [NotificationAccess]). Declaring these in the manifest alone does nothing
 * on API 23+ — without an explicit runtime request, `READ_PHONE_STATE`
 * silently fails inside `CallStateGate`'s `runCatching`, which would defeat
 * call-aware queueing without any visible error.
 */
object RuntimeGrants {
    fun hasReadPhoneState(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    fun hasPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** Permissions to request together in one system dialog. */
    fun missing(context: Context): Array<String> = buildList {
        if (!hasReadPhoneState(context)) add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotifications(context)) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()
}
