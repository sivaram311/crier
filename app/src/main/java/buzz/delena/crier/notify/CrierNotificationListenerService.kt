package buzz.delena.crier.notify

import android.app.KeyguardManager
import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import buzz.delena.crier.service.CrierForegroundService
import buzz.delena.crier.service.CrierStatusBus
import buzz.delena.crier.settings.CrierSettingsStore
import java.util.Calendar

class CrierNotificationListenerService : NotificationListenerService() {
    private val settings by lazy { CrierSettingsStore(this) }
    private val dedupe = NotificationDedupe()

    override fun onListenerConnected() {
        super.onListenerConnected()
        CrierStatusBus.update { it.copy(listenerConnected = true) }
        if (settings.assistantEnabled) {
            ContextCompat.startForegroundService(this, Intent(this, CrierForegroundService::class.java))
        }
    }

    override fun onListenerDisconnected() {
        CrierStatusBus.update { it.copy(listenerConnected = false) }
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (!settings.assistantEnabled) return
        if (!dedupe.shouldProcess(notification.key)) return

        val now = Calendar.getInstance()
        val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        if (settings.quietHoursEnabled && QuietHours.isQuiet(minutes, settings.quietStartMinutes, settings.quietEndMinutes)) return

        val n = notification.notification
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        val locked = keyguardManager?.isKeyguardLocked == true
        val isPublicVisibility = n.visibility == Notification.VISIBILITY_PUBLIC
        val publicVersion = n.publicVersion
        if (!LockScreenGate.canSpeak(locked, isPublicVisibility, publicVersion != null, settings.speakWhenLocked)) return

        val effective = if (locked && !isPublicVisibility && !settings.speakWhenLocked) publicVersion ?: n else n
        val extras = effective.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val flags = n.flags
        val ongoing = flags and Notification.FLAG_ONGOING_EVENT != 0
        val groupSummary = flags and Notification.FLAG_GROUP_SUMMARY != 0
        val fgs = flags and Notification.FLAG_FOREGROUND_SERVICE != 0

        if (!NotificationSpeechFilter.shouldSpeak(
                packageName = notification.packageName,
                ownPackage = packageName,
                allowedPackages = settings.allowedPackages(),
                title = title,
                text = text,
                isOngoing = ongoing,
                isGroupSummary = groupSummary,
                isForegroundService = fgs,
                allowAll = settings.allowAllApps,
            )
        ) {
            return
        }

        val label = runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(notification.packageName, 0),
            ).toString()
        }.getOrDefault(notification.packageName)

        val line = NotificationSpeechFilter.spokenLine(label, title, text)
        CrierPipelineBus.emit(NotificationSpeechEvent(notification.key, label, line))
    }
}
