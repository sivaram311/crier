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
 *
 * [CrierForegroundService] enqueues from its pipeline collector and drains
 * from its call-state collector — two separate coroutines that can run on
 * different `Dispatchers.IO` threads concurrently, so every method here is
 * synchronized on the same monitor to keep the backing [ArrayDeque] from
 * being mutated by two threads at once.
 */
class SpeechQueue(
    private val maxSize: Int = 20,
    private val maxAgeMs: Long = 10 * 60_000L,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val lock = Any()
    private val pending = ArrayDeque<SpeechRequest>()

    fun enqueue(request: SpeechRequest): Unit = synchronized(lock) {
        pending.addLast(request)
        while (pending.size > maxSize) {
            pending.removeFirst()
        }
    }

    /** Pops every non-stale request in arrival order; drops stale ones silently. */
    fun drainReady(now: Long = clock()): List<SpeechRequest> = synchronized(lock) {
        val ready = mutableListOf<SpeechRequest>()
        while (pending.isNotEmpty()) {
            val next = pending.removeFirst()
            if (now - next.enqueuedAtMs <= maxAgeMs) {
                ready += next
            }
        }
        ready
    }

    fun isEmpty(): Boolean = synchronized(lock) { pending.isEmpty() }

    fun size(): Int = synchronized(lock) { pending.size }
}
