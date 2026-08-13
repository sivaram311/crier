package buzz.delena.crier.notify

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** A notification that passed filtering and is ready to be spoken (or queued). */
data class NotificationSpeechEvent(
    val notificationKey: String,
    val appLabel: String,
    val spokenLine: String,
)

/**
 * In-process bridge from [CrierNotificationListenerService] to
 * [buzz.delena.crier.service.CrierForegroundService]. Same pattern as
 * forgecity-launcher's AssistantEventBridge — decouples service start
 * ordering (the listener may connect before the foreground service is up).
 */
object CrierPipelineBus {
    private val _events = MutableSharedFlow<NotificationSpeechEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<NotificationSpeechEvent> = _events.asSharedFlow()

    fun emit(event: NotificationSpeechEvent) {
        _events.tryEmit(event)
    }
}
