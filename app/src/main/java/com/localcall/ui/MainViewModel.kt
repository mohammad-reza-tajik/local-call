package com.localcall.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.localcall.LocalCallApp
import com.localcall.call.Peer

/** Thin bridge between the UI and the process-wide [com.localcall.call.CallEngine]. */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val container = app as LocalCallApp
    private val engine = container.engine
    private val identityRepository = container.identityRepository

    val identity = identityRepository.identity
    val peers = engine.peers
    val callState = engine.state
    val muted = engine.muted
    val speaker = engine.speaker
    val notices = engine.notices

    fun rename(name: String) = identityRepository.rename(name)
    fun call(peer: Peer) = engine.call(peer)
    fun accept() = engine.accept()
    fun hangup() = engine.hangup()
    fun setMuted(muted: Boolean) = engine.setMuted(muted)
    fun setSpeaker(on: Boolean) = engine.setSpeaker(on)
}
