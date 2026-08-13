package buzz.delena.crier.notify

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

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
 *
 * Backed by an unlimited [Channel] rather than a bounded `SharedFlow` +
 * `tryEmit`: the foreground service's collector synthesizes + plays audio
 * synchronously per event, so it can be slower than the rate notifications
 * arrive at. A bounded buffer with `tryEmit` would silently drop events once
 * full; `trySend` on an unlimited channel never fails, so a notification
 * burst queues up rather than vanishing (each event is a short spoken line,
 * so unbounded here is a non-issue in practice).
 */
object CrierPipelineBus {
    private val channel = Channel<NotificationSpeechEvent>(capacity = Channel.UNLIMITED)
    val events: Flow<NotificationSpeechEvent> = channel.receiveAsFlow()

    fun emit(event: NotificationSpeechEvent) {
        channel.trySend(event)
    }
}
