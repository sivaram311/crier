package buzz.delena.crier.notify

/** A line queued because a phone call was active when its notification arrived. */
data class SpeechRequest(
    val notificationKey: String,
    val line: String,
    val enqueuedAtMs: Long,
)

/**
 * Pure, Android-free FIFO queue for notifications that arrive mid-call.
 * Drained once [CallStateGate] reports the call as ended. Bounded by both
 * size and age so a long call doesn't build an unbounded backlog that gets
 * dumped all at once when the user hangs up.
 */
class SpeechQueue(
    private val maxSize: Int = 20,
    private val maxAgeMs: Long = 10 * 60_000L,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val pending = ArrayDeque<SpeechRequest>()

    fun enqueue(request: SpeechRequest) {
        pending.addLast(request)
        while (pending.size > maxSize) {
            pending.removeFirst()
        }
    }

    /** Pops every non-stale request in arrival order; drops stale ones silently. */
    fun drainReady(now: Long = clock()): List<SpeechRequest> {
        val ready = mutableListOf<SpeechRequest>()
        while (pending.isNotEmpty()) {
            val next = pending.removeFirst()
            if (now - next.enqueuedAtMs <= maxAgeMs) {
                ready += next
            }
        }
        return ready
    }

    fun isEmpty(): Boolean = pending.isEmpty()

    fun size(): Int = pending.size
}
