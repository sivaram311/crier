package buzz.delena.crier.gemini

/** What a model can actually do in this build — drives whether Playground can run it. */
enum class GeminiCapability { TTS, STT, LIVE }

data class GeminiModelOption(
    val id: String,
    val label: String,
    val capability: GeminiCapability,
    /** v0.1.0 only wires TTS end-to-end; STT/Live entries are catalog-only for now. */
    val wiredInThisBuild: Boolean,
)

/**
 * Static seed catalog for the Playground model picker. A live model-list
 * fetch (`GET /v1beta/models`) is planned for v0.2.0 so this doesn't go
 * stale as Google ships new models — see docs/ROADMAP.md.
 */
object GeminiModelCatalog {
    val TTS_MODELS = listOf(
        GeminiModelOption("gemini-2.0-flash", "Gemini 2.0 Flash", GeminiCapability.TTS, true),
        GeminiModelOption("gemini-2.0-flash-lite-preview-02-05", "Gemini 2.0 Flash Lite (preview)", GeminiCapability.TTS, true),
        GeminiModelOption("gemini-2.5-flash-preview-tts", "Gemini 2.5 Flash (TTS preview)", GeminiCapability.TTS, true),
        GeminiModelOption("gemini-2.5-pro-preview-tts", "Gemini 2.5 Pro (TTS preview)", GeminiCapability.TTS, true),
        GeminiModelOption("gemini-2.5-flash", "Gemini 2.5 Flash", GeminiCapability.TTS, true),
        GeminiModelOption("gemini-2.5-pro", "Gemini 2.5 Pro", GeminiCapability.TTS, true),
    )

    val STT_MODELS = listOf(
        GeminiModelOption("gemini-2.0-flash", "Gemini 2.0 Flash (transcription)", GeminiCapability.STT, false),
        GeminiModelOption("gemini-2.5-flash", "Gemini 2.5 Flash (transcription)", GeminiCapability.STT, false),
    )

    val LIVE_MODELS = listOf(
        GeminiModelOption("gemini-2.0-flash", "Gemini 2.0 Flash (live)", GeminiCapability.LIVE, false),
        GeminiModelOption("gemini-2.5-flash-native-audio-preview-09-2025", "Gemini 2.5 Flash Live (native audio)", GeminiCapability.LIVE, false),
        GeminiModelOption("gemini-live-2.5-flash-preview", "Gemini Live 2.5 Flash (half-cascade)", GeminiCapability.LIVE, false),
    )

    val ALL = TTS_MODELS + STT_MODELS + LIVE_MODELS

    /** Prebuilt Gemini TTS voice names, per Google's speech-generation docs. */
    val VOICES = listOf(
        "Zephyr", "Puck", "Charon", "Kore", "Fenrir", "Leda",
        "Orus", "Aoede", "Callirrhoe", "Autonoe", "Enceladus", "Iapetus",
    )

    val LANGUAGES = listOf(
        "en-US" to "English (US)",
        "en-IN" to "English (India)",
        "ta-IN" to "Tamil",
        "hi-IN" to "Hindi",
        "es-US" to "Spanish (US)",
        "fr-FR" to "French",
        "de-DE" to "German",
        "ja-JP" to "Japanese",
    )
}
