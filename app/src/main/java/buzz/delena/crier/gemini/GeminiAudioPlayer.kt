package buzz.delena.crier.gemini

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "CrierAudioPlayer"

/** Plays raw 16-bit PCM or WAV container audio returned by [GeminiTtsClient] through the device speaker/earpiece. */
class GeminiAudioPlayer {
    fun play(audioBytes: ByteArray, defaultSampleRateHz: Int = 24_000) {
        if (audioBytes.isEmpty()) return

        val parsed = parseAudio(audioBytes, defaultSampleRateHz)
        val sampleRate = parsed.sampleRate
        val channels = parsed.channels
        val rawPcm = parsed.pcm

        if (rawPcm.isEmpty() || sampleRate <= 0) return

        val channelConfig = if (channels == 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
        val bytesPerSample = 2 * channels
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)
        val bufferSize = maxOf(if (minBufferSize > 0) minBufferSize else 8192, 8192)

        var track: AudioTrack? = null
        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build(),
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.play()

            var offset = 0
            val chunkSize = 4096
            while (offset < rawPcm.size) {
                val count = minOf(chunkSize, rawPcm.size - offset)
                val written = track.write(rawPcm, offset, count)
                if (written <= 0) break
                offset += written
            }

            val durationMs = (rawPcm.size.toLong() * 1000L) / (sampleRate.toLong() * bytesPerSample.toLong())
            Thread.sleep(durationMs + 100L)
        } catch (e: Exception) {
            Log.w(TAG, "AudioTrack playback error", e)
        } finally {
            runCatching { track?.stop() }
            runCatching { track?.release() }
        }
    }

    private data class ParsedAudio(val pcm: ByteArray, val sampleRate: Int, val channels: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ParsedAudio) return false
            return sampleRate == other.sampleRate && channels == other.channels && pcm.contentEquals(other.pcm)
        }

        override fun hashCode(): Int {
            var result = pcm.contentHashCode()
            result = 31 * result + sampleRate
            result = 31 * result + channels
            return result
        }
    }

    private fun parseAudio(bytes: ByteArray, defaultSampleRate: Int): ParsedAudio {
        if (bytes.size >= 44 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'A'.code.toByte() &&
            bytes[10] == 'V'.code.toByte() && bytes[11] == 'E'.code.toByte()
        ) {
            return runCatching {
                val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                var channels = 1
                var sampleRate = defaultSampleRate
                var dataOffset = -1
                var dataSize = -1

                var cursor = 12
                while (cursor + 8 <= bytes.size) {
                    val chunkId = String(bytes, cursor, 4, Charsets.US_ASCII)
                    val chunkSize = buffer.getInt(cursor + 4)
                    if (chunkId == "fmt " && cursor + 16 <= bytes.size) {
                        channels = buffer.getShort(cursor + 10).toInt().coerceIn(1, 2)
                        sampleRate = buffer.getInt(cursor + 12).coerceIn(8_000, 48_000)
                    } else if (chunkId == "data") {
                        dataOffset = cursor + 8
                        dataSize = if (chunkSize > 0 && dataOffset + chunkSize <= bytes.size) chunkSize else bytes.size - dataOffset
                        break
                    }
                    cursor += 8 + maxOf(0, chunkSize)
                }

                if (dataOffset in 0 until bytes.size) {
                    val pcm = bytes.copyOfRange(dataOffset, minOf(bytes.size, dataOffset + dataSize))
                    ParsedAudio(pcm, sampleRate, channels)
                } else {
                    ParsedAudio(bytes.copyOfRange(44, bytes.size), sampleRate, channels)
                }
            }.getOrDefault(ParsedAudio(bytes, defaultSampleRate, 1))
        }

        return ParsedAudio(bytes, defaultSampleRate, 1)
    }
}
