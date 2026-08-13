package buzz.delena.crier.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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
import buzz.delena.crier.log.CrierLogBus
import buzz.delena.crier.notify.CallStateGate
import buzz.delena.crier.notify.CrierPipelineBus
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
 * phone calls.
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
        CrierLogBus.i(TAG, "ForegroundService started")
        CrierStatusBus.update { it.copy(foregroundServiceRunning = true) }
        callStateGate.start()
        observeCallState()
        observePipeline()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        CrierLogBus.w(TAG, "ForegroundService destroyed")
        callStateGate.stop()
        scope.cancel()
        ttsClient.close()
        CrierStatusBus.update { it.copy(foregroundServiceRunning = false) }
        super.onDestroy()
    }

    private fun observeCallState() {
        scope.launch {
            callStateGate.isCallActive.collect { active ->
                CrierLogBus.d(TAG, "CallState changed: isCallActive=$active, queuedCount=${speechQueue.size()}")
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
                    CrierLogBus.i(TAG, "Call in progress; queued speech item (total queued: ${speechQueue.size()})")
                    CrierStatusBus.update { it.copy(queuedCount = speechQueue.size()) }
                } else {
                    speak(request)
                }
            }
        }
    }

    private fun drainQueue() {
        val ready = speechQueue.drainReady()
        if (ready.isNotEmpty()) {
            CrierLogBus.i(TAG, "Draining ${ready.size} queued speech items after call ended")
        }
        CrierStatusBus.update { it.copy(queuedCount = 0) }
        ready.forEach { speak(it) }
    }

    private fun speak(request: SpeechRequest) {
        runCatching {
            val apiKey = settings.apiKey()
            if (apiKey.isNullOrBlank()) {
                val err = "API key is not set"
                CrierLogBus.w(TAG, err)
                CrierStatusBus.update { it.copy(lastError = err) }
                return@runCatching
            }

            CrierLogBus.i(
                TAG,
                "Synthesizing notification text (${request.line.length} chars) with model=${settings.ttsModel}, voice=${settings.voiceName}",
                request.line,
            )

            val result = ttsClient.synthesize(
                apiKey = apiKey,
                model = settings.ttsModel,
                prompt = request.line,
                voice = settings.voiceName,
                languageCode = settings.languageCode,
                systemPrompt = settings.systemPrompt,
            )

            when (result) {
                is GeminiAudioResult.Success -> {
                    CrierLogBus.i(TAG, "Playing audio (${result.pcm.size} bytes @ ${result.sampleRateHz}Hz)")
                    audioPlayer.play(result.pcm, result.sampleRateHz)
                    CrierStatusBus.update { it.copy(lastSpokenLine = request.line, lastError = null) }
                }
                is GeminiAudioResult.Unauthorized -> {
                    val err = "API key rejected (Unauthorized)"
                    CrierLogBus.e(TAG, err)
                    CrierStatusBus.update { it.copy(lastError = err) }
                }
                is GeminiAudioResult.ModelUnavailable -> {
                    val err = "Model unavailable for this key (${settings.ttsModel})"
                    CrierLogBus.e(TAG, err)
                    CrierStatusBus.update { it.copy(lastError = err) }
                }
                is GeminiAudioResult.Timeout -> {
                    val err = "Request timed out"
                    CrierLogBus.w(TAG, err)
                    CrierStatusBus.update { it.copy(lastError = err) }
                }
                is GeminiAudioResult.Malformed -> {
                    val err = "Malformed response from Gemini API"
                    CrierLogBus.e(TAG, err)
                    CrierStatusBus.update { it.copy(lastError = err) }
                }
                is GeminiAudioResult.Unavailable -> {
                    val err = "Service unavailable"
                    CrierLogBus.e(TAG, err)
                    CrierStatusBus.update { it.copy(lastError = err) }
                }
            }
        }.onFailure { e ->
            Log.w(TAG, "speak_failed", e)
            CrierLogBus.e(TAG, "Exception in speak(): ${e.message}", null, e.stackTraceToString())
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

        fun restart(context: Context) {
            val intent = Intent(context, CrierForegroundService::class.java)
            context.stopService(intent)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
