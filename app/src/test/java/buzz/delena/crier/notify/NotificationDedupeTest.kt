package buzz.delena.crier.notify

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDedupeTest {
    @Test
    fun `first occurrence of a key is processed`() {
        val dedupe = NotificationDedupe()
        assertTrue(dedupe.shouldProcess("a"))
    }

    @Test
    fun `repeat within ttl is suppressed`() {
        var now = 0L
        val dedupe = NotificationDedupe(ttlMs = 1_000L, clock = { now })
        assertTrue(dedupe.shouldProcess("a"))
        now += 500
        assertFalse(dedupe.shouldProcess("a"))
    }

    @Test
    fun `repeat after ttl is processed again`() {
        var now = 0L
        val dedupe = NotificationDedupe(ttlMs = 1_000L, clock = { now })
        assertTrue(dedupe.shouldProcess("a"))
        now += 1_500
        assertTrue(dedupe.shouldProcess("a"))
    }
}
