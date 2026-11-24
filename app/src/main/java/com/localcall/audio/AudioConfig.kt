package com.localcall.audio

/** 16 kHz mono PCM, sent in 20 ms frames: clear voice at ~256 kbit/s, trivial for local Wi-Fi. */
object AudioConfig {
    const val SAMPLE_RATE = 16_000
    const val FRAME_MS = 20
    const val FRAME_SAMPLES = SAMPLE_RATE * FRAME_MS / 1000
    const val FRAME_BYTES = FRAME_SAMPLES * 2
}
