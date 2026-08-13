package buzz.delena.crier.notify

import android.app.KeyguardManager
import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import buzz.delena.crier.log.CrierLogBus
import buzz.delena.crier.service.CrierForegroundService
import buzz.delena.crier.service.CrierStatusBus
import buzz.delena.crier.settings.CrierSettingsStore
import java.util.Calendar

class CrierNotificationListenerService : NotificationListenerService() {
    private val settings by lazy { CrierSettingsStore(this) }
    private val dedupe = NotificationDedupe()

    override fun onListenerConnected() {
        super.onListenerConnected()
        CrierLogBus.i(TAG, "NotificationListener connected")
        CrierStatusBus.update { it.copy(listenerConnected = true) }
        if (settings.assistantEnabled) {
            ContextCompat.startForegroundService(this, Intent(this, CrierForegroundService::class.java))
        }
    }

    override fun onListenerDisconnected() {
        CrierLogBus.w(TAG, "NotificationListener disconnected")
        CrierStatusBus.update { it.copy(listenerConnected = false) }
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val pkg = notification.packageName
        CrierLogBus.d(TAG, "onNotificationPosted from package=$pkg (key=${notification.key})")

        if (!settings.assistantEnabled) {
            CrierLogBus.d(TAG, "Ignored notification from $pkg: Assistant relay is disabled in settings")
            return
        }
        if (!dedupe.shouldProcess(notification.key)) {
            CrierLogBus.d(TAG, "Ignored duplicate/reposted notification key=${notification.key}")
            return
        }

        val now = Calendar.getInstance()
        val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        if (settings.quietHoursEnabled && QuietHours.isQuiet(minutes, settings.quietStartMinutes, settings.quietEndMinutes)) {
            CrierLogBus.d(TAG, "Ignored notification from $pkg: Active quiet hours ($minutes min)")
            return
        }

        val n = notification.notification
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        val locked = keyguardManager?.isKeyguardLocked == true
        val isPublicVisibility = n.visibility == Notification.VISIBILITY_PUBLIC
        val publicVersion = n.publicVersion
        if (!LockScreenGate.canSpeak(locked, isPublicVisibility, publicVersion != null, settings.speakWhenLocked)) {
            CrierLogBus.d(TAG, "Ignored notification from $pkg: Blocked by lock screen privacy gate (locked=$locked, speakWhenLocked=${settings.speakWhenLocked})")
            return
        }

        val effective = if (locked && !isPublicVisibility && !settings.speakWhenLocked) publicVersion ?: n else n
        val extras = effective.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extractFullNotificationText(extras)
        val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val flags = n.flags
        val ongoing = flags and Notification.FLAG_ONGOING_EVENT != 0
        val groupSummary = flags and Notification.FLAG_GROUP_SUMMARY != 0
        val fgs = flags and Notification.FLAG_FOREGROUND_SERVICE != 0

        if (!NotificationSpeechFilter.shouldSpeak(
                packageName = pkg,
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
            CrierLogBus.d(TAG, "Filtered notification from $pkg (title=$title, allowAll=${settings.allowAllApps}, allowed=${pkg in settings.allowedPackages()}, ongoing=$ongoing, fgs=$fgs)")
            return
        }

        val label = runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0),
            ).toString()
        }.getOrDefault(pkg)

        val line = NotificationSpeechFilter.spokenLine(label, title, text, subText)
        CrierLogBus.i(TAG, "Queued notification for speech: \"$line\" (pkg=$pkg)")
        CrierPipelineBus.emit(NotificationSpeechEvent(notification.key, label, line))
    }

    private fun extractFullNotificationText(extras: Bundle?): String? {
        if (extras == null) return null

        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        if (!bigText.isNullOrBlank()) return bigText

        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (!textLines.isNullOrEmpty()) {
            return textLines.filterNotNull().joinToString("\n") { it.toString() }
        }

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (!text.isNullOrBlank()) return text

        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
        if (!infoText.isNullOrBlank()) return infoText

        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
        if (!summaryText.isNullOrBlank()) return summaryText

        return null
    }

    companion object {
        private const val TAG = "CrierNotifySvc"
    }
}
