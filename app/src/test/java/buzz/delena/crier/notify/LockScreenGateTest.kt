package buzz.delena.crier.notify

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockScreenGateTest {
    @Test
    fun `unlocked always allows speaking`() {
        assertTrue(LockScreenGate.canSpeak(locked = false, isPublicVisibility = false, hasPublicVersion = false))
    }

    @Test
    fun `locked with private visibility and no public version is blocked`() {
        assertFalse(LockScreenGate.canSpeak(locked = true, isPublicVisibility = false, hasPublicVersion = false))
    }

    @Test
    fun `locked with public visibility is allowed`() {
        assertTrue(LockScreenGate.canSpeak(locked = true, isPublicVisibility = true, hasPublicVersion = false))
    }

    @Test
    fun `locked with a public version fallback is allowed`() {
        assertTrue(LockScreenGate.canSpeak(locked = true, isPublicVisibility = false, hasPublicVersion = true))
    }
}
