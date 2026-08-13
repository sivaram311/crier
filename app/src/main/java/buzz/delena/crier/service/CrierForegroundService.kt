package buzz.delena.crier.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.Context
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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

    /**
     * Runs entirely inside [runCatching]: this is called from the
     * [CrierPipelineBus] collector coroutine, and an uncaught exception here
     * (a bad sample rate reaching [GeminiAudioPlayer]'s `AudioTrack`
     * construction, for example) would otherwise terminate that coroutine
     * permanently — silently killing the whole relay until the service
     * restarts, not just failing this one notification.
     */
    private fun speak(request: SpeechRequest) {
        runCatching {
            val apiKey = settings.apiKey()
            if (apiKey.isNullOrBlank()) {
                CrierStatusBus.update { it.copy(lastError = "API key is not set") }
                return@runCatching
            }
            val result = ttsClient.synthesize(
                apiKey = apiKey,
                model = settings.ttsModel,
                prompt = request.line,
                voice = settings.voiceName,
                languageCode = settings.languageCode,
            )
            when (result) {
                is GeminiAudioResult.Success -> {
                    audioPlayer.play(result.pcm, result.sampleRateHz)
                    CrierStatusBus.update { it.copy(lastSpokenLine = request.line, lastError = null) }
                }
                is GeminiAudioResult.Unauthorized -> {
                    CrierStatusBus.update { it.copy(lastError = "API key rejected (Unauthorized)") }
                }
                is GeminiAudioResult.ModelUnavailable -> {
                    CrierStatusBus.update { it.copy(lastError = "Model unavailable for this key") }
                }
                is GeminiAudioResult.Timeout -> {
                    CrierStatusBus.update { it.copy(lastError = "Request timed out") }
                }
                is GeminiAudioResult.Malformed -> {
                    CrierStatusBus.update { it.copy(lastError = "Malformed response from Gemini API") }
                }
                is GeminiAudioResult.Unavailable -> {
                    CrierStatusBus.update { it.copy(lastError = "Service unavailable") }
                }
            }
        }.onFailure { e ->
            Log.w(TAG, "speak_failed", e)
            CrierStatusBus.update { it.copy(lastError = e.localizedMessage ?: e.message ?: "Unknown error") }
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
        private const val TAG = "CrierForegroundSvc"
        private const val NOTIFICATION_ID = 1001

        /**
         * [CallStateGate.start] only registers once, at service creation. If
         * `READ_PHONE_STATE` was denied then, registration silently no-ops
         * and nothing re-arms it later on its own. Callers (Home, after the
         * user grants that permission) are responsible for deciding whether
         * the relay should be running at all — this just restarts it so
         * `CallStateGate` re-registers with permission now in hand.
         */
        fun restart(context: Context) {
            val intent = Intent(context, CrierForegroundService::class.java)
            context.stopService(intent)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
