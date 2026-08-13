package buzz.delena.crier.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechQueueTest {
    @Test
    fun `drains in fifo order`() {
        var now = 0L
        val queue = SpeechQueue(clock = { now })
        queue.enqueue(SpeechRequest("a", "first", now))
        now += 10
        queue.enqueue(SpeechRequest("b", "second", now))

        val drained = queue.drainReady(now)
        assertEquals(listOf("first", "second"), drained.map { it.line })
        assertTrue(queue.isEmpty())
    }

    @Test
    fun `stale requests beyond max age are dropped silently`() {
        var now = 0L
        val queue = SpeechQueue(maxAgeMs = 1_000L, clock = { now })
        queue.enqueue(SpeechRequest("a", "old", now))
        now += 5_000
        queue.enqueue(SpeechRequest("b", "fresh", now))

        val drained = queue.drainReady(now)
        assertEquals(listOf("fresh"), drained.map { it.line })
    }

    @Test
    fun `bounded size drops oldest first`() {
        val queue = SpeechQueue(maxSize = 2, clock = { 0L })
        queue.enqueue(SpeechRequest("a", "one", 0L))
        queue.enqueue(SpeechRequest("b", "two", 0L))
        queue.enqueue(SpeechRequest("c", "three", 0L))

        assertEquals(2, queue.size())
        val drained = queue.drainReady(0L)
        assertEquals(listOf("two", "three"), drained.map { it.line })
    }
}
