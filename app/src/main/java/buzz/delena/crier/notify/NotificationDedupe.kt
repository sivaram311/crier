package buzz.delena.crier.notify

/** In-memory dedupe by notification key. Does not persist content. */
class NotificationDedupe(
    private val ttlMs: Long = 3_000L,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val seen = LinkedHashMap<String, Long>()

    fun shouldProcess(key: String): Boolean {
        val now = clock()
        prune(now)
        val last = seen[key]
        if (last != null && now - last < ttlMs) return false
        seen[key] = now
        return true
    }

    private fun prune(now: Long) {
        val iterator = seen.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value >= ttlMs) iterator.remove()
        }
        while (seen.size > 64) {
            val first = seen.keys.firstOrNull() ?: break
            seen.remove(first)
        }
    }
}
