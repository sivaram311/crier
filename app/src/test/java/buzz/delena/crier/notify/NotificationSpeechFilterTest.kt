package buzz.delena.crier.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSpeechFilterTest {
    @Test
    fun `own package never speaks`() {
        assertFalse(
            NotificationSpeechFilter.shouldSpeak(
                packageName = "buzz.delena.crier",
                ownPackage = "buzz.delena.crier",
                allowedPackages = setOf("buzz.delena.crier"),
                title = "t",
                text = "b",
                isOngoing = false,
                isGroupSummary = false,
                isForegroundService = false,
            ),
        )
    }

    @Test
    fun `not allowlisted does not speak when allowAll is false`() {
        assertFalse(
            NotificationSpeechFilter.shouldSpeak(
                packageName = "com.example.chat",
                ownPackage = "buzz.delena.crier",
                allowedPackages = emptySet(),
                title = "t",
                text = "b",
                isOngoing = false,
                isGroupSummary = false,
                isForegroundService = false,
                allowAll = false,
            ),
        )
    }

    @Test
    fun `allowAll true speaks even when allowlist is empty`() {
        assertTrue(
            NotificationSpeechFilter.shouldSpeak(
                packageName = "com.example.chat",
                ownPackage = "buzz.delena.crier",
                allowedPackages = emptySet(),
                title = "t",
                text = "b",
                isOngoing = false,
                isGroupSummary = false,
                isForegroundService = false,
                allowAll = true,
            ),
        )
    }

    @Test
    fun `ongoing and foreground service filtered by default but allowed when toggled off`() {
        val allow = setOf("com.example.chat")
        assertFalse(
            NotificationSpeechFilter.shouldSpeak(
                "com.example.chat", "buzz.delena.crier", allow, "t", "b",
                isOngoing = true, isGroupSummary = false, isForegroundService = false,
                filterOngoing = true,
            ),
        )
        assertTrue(
            NotificationSpeechFilter.shouldSpeak(
                "com.example.chat", "buzz.delena.crier", allow, "t", "b",
                isOngoing = true, isGroupSummary = false, isForegroundService = false,
                filterOngoing = false,
            ),
        )
        assertFalse(
            NotificationSpeechFilter.shouldSpeak(
                "com.example.chat", "buzz.delena.crier", allow, "t", "b",
                isOngoing = false, isGroupSummary = false, isForegroundService = true,
                filterForegroundServices = true,
            ),
        )
        assertTrue(
            NotificationSpeechFilter.shouldSpeak(
                "com.example.chat", "buzz.delena.crier", allow, "t", "b",
                isOngoing = false, isGroupSummary = false, isForegroundService = true,
                filterForegroundServices = false,
            ),
        )
    }

    @Test
    fun `allowlisted with content speaks`() {
        assertTrue(
            NotificationSpeechFilter.shouldSpeak(
                "com.example.chat", "buzz.delena.crier", setOf("com.example.chat"), "t", "b",
                isOngoing = false, isGroupSummary = false, isForegroundService = false,
            ),
        )
    }

    @Test
    fun `spoken line combines label title and text`() {
        assertEquals("App: Hi. There", NotificationSpeechFilter.spokenLine("App", "Hi", "There"))
        assertEquals("App: Hi", NotificationSpeechFilter.spokenLine("App", "Hi", null))
        assertEquals("App", NotificationSpeechFilter.spokenLine("App", null, null))
    }
}
