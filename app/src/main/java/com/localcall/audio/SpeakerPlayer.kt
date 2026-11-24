package com.localcall.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

/** Plays incoming PCM frames on the voice-call stream. */
@Suppress("DEPRECATION")
class SpeakerPlayer {

    private var track: AudioTrack? = null

    fun start() {
        val minBuffer = AudioTrack.getMinBufferSize(
            AudioConfig.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        // A few frames of headroom smooth out network jitter.
        track = AudioTrack(
            AudioManager.STREAM_VOICE_CALL,
            AudioConfig.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, AudioConfig.FRAME_BYTES * 6),
            AudioTrack.MODE_STREAM,
        ).also { it.play() }
    }

    fun write(pcm: ByteArray, offset: Int, length: Int) {
        track?.write(pcm, offset, length)
    }

    fun release() {
        track?.let {
            runCatching { it.stop() }
            it.release()
        }
        track = null
    }
}
