package buzz.delena.crier.notify

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietHoursTest {
    @Test
    fun `same start and end means never quiet`() {
        assertFalse(QuietHours.isQuiet(100, 500, 500))
    }

    @Test
    fun `normal same-day window`() {
        assertTrue(QuietHours.isQuiet(600, 480, 720))
        assertFalse(QuietHours.isQuiet(800, 480, 720))
    }

    @Test
    fun `wraps across midnight`() {
        val start = 22 * 60
        val end = 7 * 60
        assertTrue(QuietHours.isQuiet(23 * 60, start, end))
        assertTrue(QuietHours.isQuiet(3 * 60, start, end))
        assertFalse(QuietHours.isQuiet(12 * 60, start, end))
    }
}
