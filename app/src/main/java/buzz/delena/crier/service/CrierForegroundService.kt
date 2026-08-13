package buzz.delena.crier.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import buzz.delena.crier.CrierApp
import buzz.delena.crier.MainActivity
import buzz.delena.crier.R
import buzz.delena.crier.gemini.GeminiAudioPlayer
import buzz.delena.crier.gemini.GeminiAudioResult
import buzz.delena.crier.gemini.GeminiTtsClient
import buzz.delena.crier.notify.CallStateGate
import buzz.delena.crier.notify.CrierPipelineBus
import buzz.delena.crier.notify.NotificationSpeechEvent
import buzz.delena.crier.notify.SpeechQueue
import buzz.delena.crier.notify.SpeechRequest
import buzz.delena.crier.settings.CrierSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps the notification-voice pipeline alive in the background via a
 * persistent low-priority foreground notification, and gates speech around
 * phone calls: notifications that arrive while [CallStateGate] reports the
 * line busy are queued by [SpeechQueue] and spoken once the call ends.
 */
class CrierForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val settings by lazy { CrierSettingsStore(this) }
    private val callStateGate by lazy { CallStateGate(this) }
    private val speechQueue = SpeechQueue()
    private val ttsClient = GeminiTtsClient()
    private val audioPlayer = GeminiAudioPlayer()

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        CrierStatusBus.update { it.copy(foregroundServiceRunning = true) }
        callStateGate.start()
        observeCallState()
        observePipeline()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        callStateGate.stop()
        scope.cancel()
        ttsClient.close()
        CrierStatusBus.update { it.copy(foregroundServiceRunning = false) }
        super.onDestroy()
    }

    private fun observeCallState() {
        scope.launch {
            callStateGate.isCallActive.collect { active ->
                CrierStatusBus.update { it.copy(callActive = active, queuedCount = speechQueue.size()) }
                if (!active) drainQueue()
            }
        }
    }

    private fun observePipeline() {
        scope.launch {
            CrierPipelineBus.events.collect { event ->
                val request = SpeechRequest(event.notificationKey, event.spokenLine, System.currentTimeMillis())
                if (callStateGate.isCallActive.value) {
                    speechQueue.enqueue(request)
                    CrierStatusBus.update { it.copy(queuedCount = speechQueue.size()) }
                } else {
                    speak(request)
                }
            }
        }
    }

    private fun drainQueue() {
        val ready = speechQueue.drainReady()
        CrierStatusBus.update { it.copy(queuedCount = 0) }
        ready.forEach { speak(it) }
    }

    private fun speak(request: SpeechRequest) {
        val apiKey = settings.apiKey()
        if (apiKey.isNullOrBlank()) return
        val result = ttsClient.synthesize(
            apiKey = apiKey,
            model = settings.ttsModel,
            prompt = request.line,
            voice = settings.voiceName,
            languageCode = settings.languageCode,
        )
        if (result is GeminiAudioResult.Success) {
            audioPlayer.play(result.pcm, result.sampleRateHz)
            CrierStatusBus.update { it.copy(lastSpokenLine = request.line) }
        }
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CrierApp.FOREGROUND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_crier)
            .setContentTitle(getString(R.string.notify_channel_name))
            .setContentText(getString(R.string.notify_channel_desc))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
