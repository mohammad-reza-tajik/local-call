package com.localcall.network

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

data class Received(val message: ControlMessage, val from: InetAddress)

/**
 * One UDP socket shared by discovery and call signaling. [send] and [broadcast] do network I/O,
 * so call them from a background dispatcher.
 */
class ControlChannel {

    private val _incoming = MutableSharedFlow<Received>(extraBufferCapacity = 64)
    val incoming: SharedFlow<Received> = _incoming

    @Volatile
    private var socket: DatagramSocket? = null

    /** @throws IOException if the port cannot be opened. */
    fun start(scope: CoroutineScope) {
        if (socket != null) return
        val s = DatagramSocket(null).apply {
            reuseAddress = true
            broadcast = true
            bind(InetSocketAddress(NetworkConfig.CONTROL_PORT))
        }
        socket = s
        scope.launch(Dispatchers.IO) { receiveLoop(s) }
    }

    fun stop() {
        socket?.close()
        socket = null
    }

    fun send(message: ControlMessage, to: InetAddress) {
        val s = socket ?: return
        val data = message.encode()
        try {
            s.send(DatagramPacket(data, data.size, to, NetworkConfig.CONTROL_PORT))
        } catch (_: IOException) {
            // Transient network error; callers retry where it matters.
        }
    }

    fun broadcast(message: ControlMessage) {
        NetworkUtils.broadcastAddresses().forEach { send(message, it) }
    }

    private fun receiveLoop(s: DatagramSocket) {
        val buffer = ByteArray(512)
        val packet = DatagramPacket(buffer, buffer.size)
        while (!s.isClosed) {
            try {
                packet.length = buffer.size
                s.receive(packet)
                ControlMessage.decode(packet.data, packet.length)
                    ?.let { _incoming.tryEmit(Received(it, packet.address)) }
            } catch (_: IOException) {
                if (s.isClosed) break
            }
        }
    }
}
