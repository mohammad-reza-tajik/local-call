package com.localcall.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** Streams microphone audio as PCM frames, with echo cancellation and noise suppression if available. */
class MicCapture {

    @SuppressLint("MissingPermission")
    fun frames(): Flow<ByteArray> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(
            AudioConfig.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            AudioConfig.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, AudioConfig.FRAME_BYTES * 4),
        )
        check(record.state == AudioRecord.STATE_INITIALIZED) { "Microphone unavailable" }

        val effects = attachEffects(record.audioSessionId)
        try {
            record.startRecording()
            val buffer = ByteArray(AudioConfig.FRAME_BYTES)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = record.read(buffer, 0, buffer.size)
                check(read >= 0) { "Microphone read failed: $read" }
                if (read > 0) emit(buffer.copyOf(read))
            }
        } finally {
            effects.forEach { it.release() }
            record.release()
        }
    }.flowOn(Dispatchers.IO)

    private fun attachEffects(session: Int): List<AudioEffect> = buildList {
        if (AcousticEchoCanceler.isAvailable()) {
            AcousticEchoCanceler.create(session)?.let { it.enabled = true; add(it) }
        }
        if (NoiseSuppressor.isAvailable()) {
            NoiseSuppressor.create(session)?.let { it.enabled = true; add(it) }
        }
    }
}
