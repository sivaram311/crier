package buzz.delena.crier.gemini

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.util.Base64
import javax.net.ssl.HttpsURLConnection

private const val TAG = "CrierGemini"

/**
 * Gemini native audio TTS via `generateContent` with `responseModalities: ["AUDIO"]`.
 * Adapted from forgecity-launcher's proven GeminiAudioTtsClient — same request
 * shape and response parsing, trimmed of the launcher-specific diagnostics ring
 * buffer in favor of plain Log calls.
 */
class GeminiTtsClient(
    private val connectTimeoutMs: Int = 8_000,
    private val readTimeoutMs: Int = 60_000,
    private val maxResponseBytes: Int = 12 * 1024 * 1024,
    private val maxAttempts: Int = 2,
) : AutoCloseable {
    @Volatile
    private var closed = false

    fun synthesize(
        apiKey: String,
        model: String,
        prompt: String,
        voice: String = DEFAULT_VOICE,
        languageCode: String = DEFAULT_LANGUAGE,
    ): GeminiAudioResult {
        if (closed || apiKey.isBlank() || prompt.isBlank()) return GeminiAudioResult.Unavailable
        val modelId = normalizeTtsModel(model)
        val voiceName = voice.trim().ifBlank { DEFAULT_VOICE }
        val lang = languageCode.trim().ifBlank { DEFAULT_LANGUAGE }
        val spokenPrompt = applyLanguageHint(prompt.trim(), lang)
        val url = validatedUrl(modelId) ?: return GeminiAudioResult.Unavailable

        var last: GeminiAudioResult = GeminiAudioResult.Unavailable
        repeat(maxAttempts) { attempt ->
            val result = synthesizeOnce(url, apiKey.trim(), modelId, spokenPrompt, voiceName, attempt + 1)
            last = result
            when (result) {
                is GeminiAudioResult.Success -> return result
                is GeminiAudioResult.Unavailable,
                is GeminiAudioResult.Malformed,
                is GeminiAudioResult.Timeout,
                -> {
                    if (attempt + 1 < maxAttempts) {
                        try {
                            Thread.sleep(350L * (attempt + 1))
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                            return GeminiAudioResult.Timeout
                        }
                    }
                }
                else -> return result
            }
        }
        return last
    }

    private fun synthesizeOnce(
        url: URL,
        apiKey: String,
        modelId: String,
        prompt: String,
        voiceName: String,
        attempt: Int,
    ): GeminiAudioResult {
        var connection: HttpsURLConnection? = null
        return try {
            connection = (url.openConnection() as? HttpsURLConnection)
                ?: return GeminiAudioResult.Unavailable
            connection.requestMethod = "POST"
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("x-goog-api-key", apiKey)
            val payload = buildRequestBody(prompt, voiceName).toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(payload.size)
            connection.outputStream.use { it.write(payload) }
            val status = connection.responseCode
            Log.d(TAG, "gemini_tts_http status=$status model=$modelId attempt=$attempt")
            when (status) {
                HttpURLConnection.HTTP_OK -> {
                    val body = readBounded(connection.inputStream) ?: return GeminiAudioResult.Malformed
                    parseSuccess(body.toString(Charsets.UTF_8))
                }
                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN ->
                    GeminiAudioResult.Unauthorized
                HttpURLConnection.HTTP_BAD_REQUEST -> {
                    val err = readBounded(connection.errorStream)?.toString(Charsets.UTF_8).orEmpty()
                    classifyBadRequest(err)
                }
                HttpURLConnection.HTTP_NOT_FOUND -> GeminiAudioResult.ModelUnavailable
                HttpURLConnection.HTTP_GATEWAY_TIMEOUT, 429 -> GeminiAudioResult.Timeout
                500, 502, 503 -> GeminiAudioResult.Unavailable
                else -> GeminiAudioResult.Unavailable
            }
        } catch (_: SocketTimeoutException) {
            GeminiAudioResult.Timeout
        } catch (_: InterruptedIOException) {
            GeminiAudioResult.Timeout
        } catch (e: Exception) {
            Log.w(TAG, "gemini_tts_unavailable", e)
            GeminiAudioResult.Unavailable
        } finally {
            connection?.disconnect()
        }
    }

    override fun close() {
        closed = true
    }

    private fun validatedUrl(model: String): URL? = runCatching {
        val encoded = URLEncoder.encode(model, Charsets.UTF_8.name()).replace("+", "%20")
        URL("https://generativelanguage.googleapis.com/v1beta/models/$encoded:generateContent")
    }.getOrNull()

    internal fun buildRequestBody(prompt: String, voice: String): String {
        val escapedPrompt = escapeJson(prompt)
        val escapedVoice = escapeJson(voice)
        return """{"contents":[{"parts":[{"text":"$escapedPrompt"}]}],"generationConfig":{"responseModalities":["AUDIO"],"speechConfig":{"voiceConfig":{"prebuiltVoiceConfig":{"voiceName":"$escapedVoice"}}}}}"""
    }

    private fun escapeJson(value: String): String = buildString(value.length + 8) {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch.code < 0x20) append("\\u%04x".format(ch.code)) else append(ch)
            }
        }
    }

    private fun parseSuccess(json: String): GeminiAudioResult {
        val audio = GeminiAudioResponseParser.extract(json)
            ?: return GeminiAudioResult.Malformed
        if (audio.pcm.size < 64) return GeminiAudioResult.Malformed
        return GeminiAudioResult.Success(audio.pcm, audio.sampleRateHz, audio.mimeType)
    }

    private fun classifyBadRequest(err: String): GeminiAudioResult = when {
        err.contains("API_KEY_INVALID", true) || err.contains("API key not valid", true) ->
            GeminiAudioResult.Unauthorized
        err.contains("not found", true) || err.contains("NOT_FOUND", true) ->
            GeminiAudioResult.ModelUnavailable
        else -> GeminiAudioResult.Unavailable
    }

    private fun readBounded(input: java.io.InputStream?): ByteArray? {
        if (input == null) return null
        input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8_192)
            var total = 0
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                if (total > maxResponseBytes) return null
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }
    }

    companion object {
        const val DEFAULT_TTS_MODEL = "gemini-2.5-flash-preview-tts"
        const val DEFAULT_VOICE = "Kore"
        const val DEFAULT_LANGUAGE = "en-US"

        fun normalizeTtsModel(raw: String): String =
            raw.trim().removePrefix("models/").ifBlank { DEFAULT_TTS_MODEL }

        fun applyLanguageHint(prompt: String, languageCode: String): String {
            val lang = languageCode.trim().ifBlank { DEFAULT_LANGUAGE }
            return "Synthesize speech only (audio output, no text). Speak in $lang.\n$prompt"
        }
    }
}

sealed interface GeminiAudioResult {
    data class Success(val pcm: ByteArray, val sampleRateHz: Int, val mimeType: String) : GeminiAudioResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            return sampleRateHz == other.sampleRateHz && mimeType == other.mimeType && pcm.contentEquals(other.pcm)
        }

        override fun hashCode(): Int {
            var result = pcm.contentHashCode()
            result = 31 * result + sampleRateHz
            result = 31 * result + mimeType.hashCode()
            return result
        }
    }

    data object Unavailable : GeminiAudioResult
    data object ModelUnavailable : GeminiAudioResult
    data object Timeout : GeminiAudioResult
    data object Unauthorized : GeminiAudioResult
    data object Malformed : GeminiAudioResult
}

data class GeminiAudioPayload(val pcm: ByteArray, val sampleRateHz: Int, val mimeType: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeminiAudioPayload) return false
        return sampleRateHz == other.sampleRateHz && mimeType == other.mimeType && pcm.contentEquals(other.pcm)
    }

    override fun hashCode(): Int {
        var result = pcm.contentHashCode()
        result = 31 * result + sampleRateHz
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

internal object GeminiAudioResponseParser {
    private val RATE_REGEX = Regex("""rate\s*=\s*(\d+)""", RegexOption.IGNORE_CASE)

    fun extract(json: String): GeminiAudioPayload? {
        for (region in inlineDataRegions(json)) {
            val mimeType = extractJsonString(region, "mimeType") ?: "audio/L16;rate=24000"
            val b64 = extractJsonString(region, "data") ?: continue
            val pcm = runCatching { Base64.getMimeDecoder().decode(b64) }.getOrNull() ?: continue
            if (pcm.isEmpty()) continue
            val rate = RATE_REGEX.find(mimeType)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 24_000
            return GeminiAudioPayload(pcm, rate.coerceIn(8_000, 48_000), mimeType)
        }
        return null
    }

    private fun inlineDataRegions(json: String): List<String> {
        val out = mutableListOf<String>()
        val marker = "\"inlineData\""
        var from = 0
        while (true) {
            val start = json.indexOf(marker, from)
            if (start < 0) break
            val brace = json.indexOf('{', start)
            if (brace < 0) break
            var depth = 0
            var end = -1
            for (i in brace until json.length) {
                when (json[i]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            end = i
                            break
                        }
                    }
                }
            }
            if (end > brace) {
                out += json.substring(brace, end + 1)
                from = end + 1
            } else {
                break
            }
        }
        return out
    }

    fun extractJsonString(json: String, key: String): String? {
        val marker = "\"$key\""
        val index = json.indexOf(marker)
        if (index < 0) return null
        var cursor = index + marker.length
        while (cursor < json.length && json[cursor].isWhitespace()) cursor++
        if (cursor >= json.length || json[cursor] != ':') return null
        cursor++
        while (cursor < json.length && json[cursor].isWhitespace()) cursor++
        if (cursor >= json.length || json[cursor] != '"') return null
        cursor++
        val output = StringBuilder()
        while (cursor < json.length) {
            when (val ch = json[cursor++]) {
                '"' -> return output.toString()
                '\\' -> {
                    if (cursor >= json.length) return null
                    when (val escaped = json[cursor++]) {
                        '"', '\\', '/' -> output.append(escaped)
                        'n' -> output.append('\n')
                        'r' -> output.append('\r')
                        't' -> output.append('\t')
                        'u' -> {
                            if (cursor + 4 > json.length) return null
                            val code = json.substring(cursor, cursor + 4).toIntOrNull(16) ?: return null
                            output.append(code.toChar())
                            cursor += 4
                        }
                        else -> return null
                    }
                }
                else -> output.append(ch)
            }
        }
        return null
    }
}
