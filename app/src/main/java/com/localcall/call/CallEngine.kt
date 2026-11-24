package com.localcall.call

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.localcall.audio.AudioRouter
import com.localcall.audio.VoiceStream
import com.localcall.data.IdentityRepository
import com.localcall.network.ControlChannel
import com.localcall.network.ControlMessage
import com.localcall.network.MessageType
import com.localcall.network.PeerDiscovery
import com.localcall.network.Received
import com.localcall.network.WifiLocks
import com.localcall.service.CallService
import java.io.IOException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

/**
 * Owns the whole call lifecycle: discovery, ringing, accepting, talking and hanging up.
 *
 * All work runs on a single-threaded dispatcher, so the state machine below needs no locks.
 * The public functions are safe to call from the UI thread.
 */
class CallEngine(
    context: Context,
    private val identityRepository: IdentityRepository,
) {
    private val appContext = context.applicationContext

    private val scope = CoroutineScope(
        SupervisorJob() +
            Dispatchers.IO.limitedParallelism(1) +
            CoroutineExceptionHandler { _, error -> Log.e(TAG, "Engine error", error) },
    )

    private val channel = ControlChannel()
    private val discovery = PeerDiscovery(channel) { identityRepository.identity.value }
    private val router = AudioRouter(appContext)
    private val wifiLocks = WifiLocks(appContext)

    private val _state = MutableStateFlow<CallState>(CallState.Idle)
    val state: StateFlow<CallState> = _state.asStateFlow()

    val peers: StateFlow<List<Peer>> = discovery.peers

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val _speaker = MutableStateFlow(false)
    val speaker: StateFlow<Boolean> = _speaker.asStateFlow()

    private val _notices = MutableSharedFlow<Notice>(extraBufferCapacity = 8)
    val notices: SharedFlow<Notice> = _notices.asSharedFlow()

    private var appVisible = false
    private var networkJob: Job? = null
    private var callJob: Job? = null
    private var voice: VoiceStream? = null

    // region Public API

    /** Discovery runs while the app is on screen, and for as long as a call is in progress. */
    fun setAppVisible(visible: Boolean) {
        scope.launch {
            appVisible = visible
            syncNetwork()
        }
    }

    fun call(peer: Peer) {
        scope.launch {
            if (_state.value !is CallState.Idle) return@launch
            if (!hasMicPermission()) return@launch notify(Notice.MicError)

            _state.value = CallState.Outgoing(peer)
            CallService.start(appContext)
            callJob = scope.launch {
                val deadline = SystemClock.elapsedRealtime() + RING_TIMEOUT_MS
                while (SystemClock.elapsedRealtime() < deadline) {
                    send(MessageType.CALL, peer)
                    delay(CALL_RETRY_MS)
                }
                sendBye(peer)
                finishCall(Notice.NoAnswer)
            }
        }
    }

    fun accept() {
        scope.launch {
            val incoming = _state.value as? CallState.Incoming ?: return@launch
            if (!hasMicPermission()) {
                send(MessageType.REJECT, incoming.peer)
                finishCall(Notice.MicError)
                return@launch
            }
            send(MessageType.ACCEPT, incoming.peer)
            CallService.start(appContext)
            startActive(incoming.peer)
        }
    }

    /** Ends, cancels or declines the current call, whichever applies. */
    fun hangup() {
        scope.launch {
            when (val current = _state.value) {
                is CallState.Idle -> Unit
                is CallState.Incoming -> send(MessageType.REJECT, current.peer)
                is CallState.Outgoing -> sendBye(current.peer)
                is CallState.Active -> sendBye(current.peer)
            }
            if (_state.value !is CallState.Idle) finishCall(notice = null)
        }
    }

    fun setMuted(muted: Boolean) {
        _muted.value = muted
    }

    fun setSpeaker(on: Boolean) {
        _speaker.value = on
        scope.launch { if (_state.value is CallState.Active) router.setSpeaker(on) }
    }

    // endregion

    // region Incoming messages

    private fun onControl(received: Received) {
        val (message, from) = received
        if (message.senderId == identityRepository.identity.value.id) return

        val sender = Peer(message.senderId, message.senderName, from, SystemClock.elapsedRealtime())
        val current = _state.value

        when (message.type) {
            MessageType.HELLO -> Unit // handled by PeerDiscovery
            MessageType.CALL -> onCallRequest(sender, current)
            MessageType.ACCEPT ->
                if (current is CallState.Outgoing && current.peer.id == sender.id) startActive(sender)
            MessageType.REJECT ->
                if (current is CallState.Outgoing && current.peer.id == sender.id) finishCall(Notice.Rejected)
            MessageType.BYE ->
                if (current.peerOrNull?.id == sender.id) {
                    finishCall(if (current is CallState.Incoming) Notice.Missed else Notice.RemoteEnded)
                }
        }
    }

    private fun onCallRequest(sender: Peer, current: CallState) {
        when {
            current is CallState.Idle -> {
                _state.value = CallState.Incoming(sender)
                callJob = scope.launch {
                    delay(RING_TIMEOUT_MS)
                    if (_state.value is CallState.Incoming) finishCall(Notice.Missed)
                }
            }
            // Both sides dialed each other at once: just connect.
            current is CallState.Outgoing && current.peer.id == sender.id -> {
                send(MessageType.ACCEPT, sender)
                startActive(sender)
            }
            // Our ACCEPT was lost and the caller is still ringing: confirm again.
            current is CallState.Active && current.peer.id == sender.id ->
                send(MessageType.ACCEPT, sender)
            // Already ringing for this peer.
            current.peerOrNull?.id == sender.id -> Unit
            // Busy with someone else.
            else -> send(MessageType.REJECT, sender)
        }
    }

    // endregion

    // region Call lifecycle

    private fun startActive(peer: Peer) {
        callJob?.cancel()
        router.enterCallMode(speakerOn = _speaker.value)

        val stream = VoiceStream(peer.address, isMuted = { _muted.value })
        stream.start(scope) { error ->
            scope.launch {
                if (_state.value is CallState.Active) {
                    sendBye(peer)
                    finishCall(if (error is IOException) Notice.NetworkError else Notice.MicError)
                }
            }
        }
        voice = stream
        _state.value = CallState.Active(peer, startedAt = SystemClock.elapsedRealtime())

        callJob = scope.launch {
            while (true) {
                delay(WATCHDOG_INTERVAL_MS)
                if (SystemClock.elapsedRealtime() - stream.lastHeardAt > LOSS_TIMEOUT_MS) {
                    finishCall(Notice.ConnectionLost)
                    break
                }
            }
        }
    }

    private fun finishCall(notice: Notice?) {
        callJob?.cancel()
        callJob = null
        voice?.stop()
        voice = null
        router.exitCallMode()
        _muted.value = false
        _speaker.value = false
        _state.value = CallState.Idle
        CallService.stop(appContext)
        notice?.let(::notify)
        syncNetwork()
    }

    // endregion

    // region Networking

    private fun syncNetwork() {
        if (appVisible || _state.value !is CallState.Idle) startNetwork() else stopNetwork()
    }

    private fun startNetwork() {
        if (networkJob != null) return
        val job = SupervisorJob(scope.coroutineContext[Job])
        val networkScope = scope + job
        try {
            channel.start(networkScope)
        } catch (_: IOException) {
            return notify(Notice.NetworkError)
        }
        wifiLocks.acquire()
        discovery.start(networkScope)
        networkScope.launch { channel.incoming.collect { onControl(it) } }
        networkJob = job
    }

    private fun stopNetwork() {
        val job = networkJob ?: return
        job.cancel()
        networkJob = null
        channel.stop()
        discovery.clear()
        wifiLocks.release()
    }

    private fun send(type: MessageType, peer: Peer) {
        val me = identityRepository.identity.value
        channel.send(ControlMessage(type, me.id, me.name), peer.address)
    }

    /** Sent a few times because UDP can drop packets; the peer's watchdog is the final fallback. */
    private fun sendBye(peer: Peer) = repeat(BYE_REPEATS) { send(MessageType.BYE, peer) }

    // endregion

    private fun notify(notice: Notice) {
        _notices.tryEmit(notice)
    }

    private fun hasMicPermission() = ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val TAG = "CallEngine"
        const val RING_TIMEOUT_MS = 30_000L
        const val CALL_RETRY_MS = 1_000L
        const val WATCHDOG_INTERVAL_MS = 1_000L
        const val LOSS_TIMEOUT_MS = 8_000L
        const val BYE_REPEATS = 3
    }
}
