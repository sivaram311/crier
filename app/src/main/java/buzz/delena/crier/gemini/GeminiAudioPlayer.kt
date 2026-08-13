package buzz.delena.crier.gemini

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/** Plays raw 16-bit PCM returned by [GeminiTtsClient] through the device speaker/earpiece. */
class GeminiAudioPlayer {
    fun play(pcm: ByteArray, sampleRateHz: Int) {
        if (pcm.isEmpty()) return
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize <= 0) return
        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRateHz)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build(),
            maxOf(minBufferSize, pcm.size),
            AudioTrack.MODE_STATIC,
            AudioManagerSessionId.generate(),
        )
        runCatching {
            track.write(pcm, 0, pcm.size)
            track.play()
            val durationMs = (pcm.size.toLong() * 1000L) / (sampleRateHz.toLong() * 2L)
            Thread.sleep(durationMs + 150L)
        }
        runCatching { track.stop() }
        track.release()
    }
}

private object AudioManagerSessionId {
    fun generate(): Int = android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
}
