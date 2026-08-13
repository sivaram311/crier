package buzz.delena.crier.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class CrierStatus(
    val listenerConnected: Boolean = false,
    val foregroundServiceRunning: Boolean = false,
    val callActive: Boolean = false,
    val queuedCount: Int = 0,
    val lastSpokenLine: String? = null,
    val lastError: String? = null,
)

/** In-process, UI-observable status so the Home screen reflects the live pipeline state. */
object CrierStatusBus {
    private val _status = MutableStateFlow(CrierStatus())
    val status: StateFlow<CrierStatus> = _status

    fun update(transform: (CrierStatus) -> CrierStatus) {
        _status.value = transform(_status.value)
    }
}
