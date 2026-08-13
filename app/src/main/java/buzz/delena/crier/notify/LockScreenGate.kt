package buzz.delena.crier.notify

/**
 * Fails closed while the device is locked: only speaks notification content
 * Android itself would already reveal on the lock screen (`VISIBILITY_PUBLIC`,
 * or the notification's own `publicVersion`). Most apps default to PRIVATE
 * visibility, so this intentionally skips most notifications while locked
 * rather than risk reading OTPs or message previews aloud — and sending that
 * content to Gemini — while the phone isn't actually unlocked.
 */
object LockScreenGate {
    fun canSpeak(locked: Boolean, isPublicVisibility: Boolean, hasPublicVersion: Boolean): Boolean {
        if (!locked) return true
        return isPublicVisibility || hasPublicVersion
    }
}
