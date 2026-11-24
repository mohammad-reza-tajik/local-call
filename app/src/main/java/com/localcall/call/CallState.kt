package com.localcall.call

sealed interface CallState {
    data object Idle : CallState
    data class Outgoing(val peer: Peer) : CallState
    data class Incoming(val peer: Peer) : CallState
    data class Active(val peer: Peer, val startedAt: Long) : CallState
}

val CallState.peerOrNull: Peer?
    get() = when (this) {
        CallState.Idle -> null
        is CallState.Outgoing -> peer
        is CallState.Incoming -> peer
        is CallState.Active -> peer
    }

/** One-off events the UI shows briefly, such as a snackbar. */
enum class Notice {
    Rejected,
    NoAnswer,
    Missed,
    RemoteEnded,
    ConnectionLost,
    MicError,
    NetworkError,
}
