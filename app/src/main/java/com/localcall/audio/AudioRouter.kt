package com.localcall.audio

import android.content.Context
import android.media.AudioManager

/** Puts the phone into call audio mode and switches between earpiece and loudspeaker. */
@Suppress("DEPRECATION")
class AudioRouter(context: Context) {

    private val audioManager = context.applicationContext
        .getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun enterCallMode(speakerOn: Boolean) {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = speakerOn
    }

    fun setSpeaker(on: Boolean) {
        audioManager.isSpeakerphoneOn = on
    }

    fun exitCallMode() {
        audioManager.isSpeakerphoneOn = false
        audioManager.mode = AudioManager.MODE_NORMAL
    }
}
