package com.localcall.network

import android.os.SystemClock
import com.localcall.call.Identity
import com.localcall.call.Peer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Announces this device with periodic broadcasts and keeps a live list of the others. */
class PeerDiscovery(
    private val channel: ControlChannel,
    private val identity: () -> Identity,
) {
    private val _peers = MutableStateFlow<List<Peer>>(emptyList())
    val peers: StateFlow<List<Peer>> = _peers

    fun start(scope: CoroutineScope) {
        scope.launch {
            while (isActive) {
                val me = identity()
                channel.broadcast(ControlMessage(MessageType.HELLO, me.id, me.name))
                delay(ANNOUNCE_INTERVAL_MS)
            }
        }
        scope.launch {
            channel.incoming.collect { (message, from) ->
                if (message.type == MessageType.HELLO && message.senderId != identity().id) {
                    upsert(Peer(message.senderId, message.senderName, from, SystemClock.elapsedRealtime()))
                }
            }
        }
        scope.launch {
            while (isActive) {
                delay(ANNOUNCE_INTERVAL_MS)
                prune()
            }
        }
    }

    fun clear() {
        _peers.value = emptyList()
    }

    private fun upsert(peer: Peer) = _peers.update { list ->
        (list.filterNot { it.id == peer.id } + peer).sortedBy { it.name.lowercase() }
    }

    private fun prune() {
        val cutoff = SystemClock.elapsedRealtime() - PEER_TIMEOUT_MS
        _peers.update { list -> list.filter { it.lastSeen >= cutoff } }
    }

    private companion object {
        const val ANNOUNCE_INTERVAL_MS = 2_000L
        const val PEER_TIMEOUT_MS = 7_000L
    }
}
