package buzz.delena.crier.notify

/**
 * Gating speech on lock screen based on user settings and notification visibility.
 */
object LockScreenGate {
    fun canSpeak(
        locked: Boolean,
        isPublicVisibility: Boolean,
        hasPublicVersion: Boolean,
        speakWhenLocked: Boolean = true,
    ): Boolean {
        if (!locked) return true
        if (speakWhenLocked) return true
        return isPublicVisibility || hasPublicVersion
    }
}
