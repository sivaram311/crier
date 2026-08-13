package buzz.delena.crier.notify

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockScreenGateTest {
    @Test
    fun `unlocked always allows speaking`() {
        assertTrue(LockScreenGate.canSpeak(locked = false, isPublicVisibility = false, hasPublicVersion = false, speakWhenLocked = false))
        assertTrue(LockScreenGate.canSpeak(locked = false, isPublicVisibility = false, hasPublicVersion = false, speakWhenLocked = true))
    }

    @Test
    fun `locked with private visibility and speakWhenLocked true is allowed`() {
        assertTrue(LockScreenGate.canSpeak(locked = true, isPublicVisibility = false, hasPublicVersion = false, speakWhenLocked = true))
    }

    @Test
    fun `locked with private visibility and speakWhenLocked false is blocked`() {
        assertFalse(LockScreenGate.canSpeak(locked = true, isPublicVisibility = false, hasPublicVersion = false, speakWhenLocked = false))
    }

    @Test
    fun `locked with public visibility is allowed even if speakWhenLocked false`() {
        assertTrue(LockScreenGate.canSpeak(locked = true, isPublicVisibility = true, hasPublicVersion = false, speakWhenLocked = false))
    }

    @Test
    fun `locked with a public version fallback is allowed even if speakWhenLocked false`() {
        assertTrue(LockScreenGate.canSpeak(locked = true, isPublicVisibility = false, hasPublicVersion = true, speakWhenLocked = false))
    }
}
