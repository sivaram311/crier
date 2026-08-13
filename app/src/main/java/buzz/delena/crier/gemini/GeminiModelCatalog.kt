package buzz.delena.crier.gemini

import buzz.delena.crier.log.CrierLogBus
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** What a model can actually do in this build — drives whether Playground can run it. */
enum class GeminiCapability { TTS, STT, LIVE }

data class GeminiModelOption(
    val id: String,
    val label: String,
    val capability: GeminiCapability,
    val wiredInThisBuild: Boolean = true,
)

/**
 * Seed catalog and dynamic live model discovery via `GET /v1beta/models`.
 */
object GeminiModelCatalog {
    val DEFAULT_TTS_MODELS = listOf(
        GeminiModelOption("gemini-3.1-flash-tts-preview", "Gemini 3.1 Flash TTS Preview (Native Audio)", GeminiCapability.TTS, true),
        GeminiModelOption("gemini-2.5-flash-preview-tts", "Gemini 2.5 Flash Preview TTS", GeminiCapability.TTS, true),
        GeminiModelOption("gemini-2.5-pro-preview-tts", "Gemini 2.5 Pro Preview TTS", GeminiCapability.TTS, true),
        GeminiModelOption("gemini-2.0-flash", "Gemini 2.0 Flash", GeminiCapability.TTS, true),
        GeminiModelOption("gemini-2.5-flash", "Gemini 2.5 Flash", GeminiCapability.TTS, true),
        GeminiModelOption("gemini-2.5-pro", "Gemini 2.5 Pro", GeminiCapability.TTS, true),
        GeminiModelOption("gemini-3.7-flash", "Gemini 3.7 Flash", GeminiCapability.TTS, true),
        GeminiModelOption("gemini-3.5-flash", "Gemini 3.5 Flash", GeminiCapability.TTS, true),
        GeminiModelOption("gemini-flash-latest", "Gemini Flash Latest", GeminiCapability.TTS, true),
        GeminiModelOption("gemini-pro-latest", "Gemini Pro Latest", GeminiCapability.TTS, true),
    )

    private var dynamicTtsModels: List<GeminiModelOption>? = null

    val TTS_MODELS: List<GeminiModelOption>
        get() = dynamicTtsModels ?: DEFAULT_TTS_MODELS

    val STT_MODELS = listOf(
        GeminiModelOption("gemini-2.0-flash", "Gemini 2.0 Flash (transcription)", GeminiCapability.STT, false),
        GeminiModelOption("gemini-2.5-flash", "Gemini 2.5 Flash (transcription)", GeminiCapability.STT, false),
    )

    val LIVE_MODELS = listOf(
        GeminiModelOption("gemini-2.5-flash-native-audio-latest", "Gemini 2.5 Flash Native Audio Latest", GeminiCapability.LIVE, false),
        GeminiModelOption("gemini-2.5-flash-native-audio-preview-09-2025", "Gemini 2.5 Flash Live (native audio)", GeminiCapability.LIVE, false),
        GeminiModelOption("gemini-live-2.5-flash-preview", "Gemini Live 2.5 Flash (half-cascade)", GeminiCapability.LIVE, false),
    )

    val ALL: List<GeminiModelOption>
        get() = TTS_MODELS + STT_MODELS + LIVE_MODELS

    val VOICES = listOf(
        "Puck", "Charon", "Kore", "Fenrir", "Aoede", "Leda",
        "Orus", "Zephyr", "Callirrhoe", "Autonoe", "Enceladus", "Iapetus",
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

    fun fetchAvailableModels(apiKey: String): List<GeminiModelOption> {
        if (apiKey.isBlank()) return TTS_MODELS
        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey.trim()}")
            val conn = url.openConnection() as HttpsURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Content-Type", "application/json")

            val status = conn.responseCode
            val responseBody = if (status == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            conn.disconnect()

            CrierLogBus.d("GeminiCatalog", "fetchAvailableModels status=$status", "GET /v1beta/models", responseBody)

            if (status == HttpURLConnection.HTTP_OK) {
                val json = JSONObject(responseBody)
                val modelsArray = json.optJSONArray("models")
                if (modelsArray != null && modelsArray.length() > 0) {
                    val list = mutableListOf<GeminiModelOption>()
                    for (i in 0 until modelsArray.length()) {
                        val m = modelsArray.getJSONObject(i)
                        val rawName = m.optString("name")
                        val displayName = m.optString("displayName", rawName)
                        val supportedMethods = m.optJSONArray("supportedGenerationMethods")
                        val methodsList = (0 until (supportedMethods?.length() ?: 0)).map {
                            supportedMethods!!.getString(it)
                        }

                        val id = rawName.removePrefix("models/")
                        if (methodsList.contains("generateContent") && !id.contains("image") && !id.contains("embedding") && !id.contains("gemma")) {
                            list += GeminiModelOption(
                                id = id,
                                label = if (displayName.isNotBlank()) "$displayName ($id)" else id,
                                capability = GeminiCapability.TTS,
                                wiredInThisBuild = true,
                            )
                        }
                    }

                    if (list.isNotEmpty()) {
                        val sorted = list.sortedWith(
                            compareByDescending<GeminiModelOption> { it.id.contains("tts", ignoreCase = true) }
                                .thenByDescending { it.id.contains("3.1-flash", ignoreCase = true) }
                                .thenByDescending { it.id.contains("2.5-flash", ignoreCase = true) }
                                .thenByDescending { it.id.contains("2.0-flash", ignoreCase = true) }
                                .thenByDescending { it.id.contains("3.7-flash", ignoreCase = true) }
                                .thenBy { it.label }
                        )
                        dynamicTtsModels = sorted
                        return sorted
                    }
                }
            }
            TTS_MODELS
        } catch (e: Exception) {
            CrierLogBus.w("GeminiCatalog", "Failed to fetch live models: ${e.message}", null, null)
            TTS_MODELS
        }
    }
}
