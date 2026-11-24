package com.localcall.audio

import android.os.SystemClock
import com.localcall.network.NetworkConfig
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * Full-duplex voice with one peer over UDP.
 *
 * Packet layout: 2-byte big-endian sequence number, then 16-bit PCM. A packet with no PCM is a
 * keep-alive (sent while muted) so the other side can tell the link is still up.
 */
class VoiceStream(
    private val remote: InetAddress,
    private val isMuted: () -> Boolean,
    private val mic: MicCapture = MicCapture(),
) {
    /** Last time any packet from the peer arrived (SystemClock.elapsedRealtime). */
    @Volatile
    var lastHeardAt: Long = SystemClock.elapsedRealtime()
        private set

    private var job: Job? = null

    fun start(scope: CoroutineScope, onFailure: (Throwable) -> Unit) {
        job = scope.launch(Dispatchers.IO) {
            val socket = DatagramSocket(null).apply {
                reuseAddress = true
                soTimeout = RECEIVE_TIMEOUT_MS
                bind(InetSocketAddress(NetworkConfig.AUDIO_PORT))
            }
            try {
                coroutineScope {
                    launch { sendLoop(socket) }
                    launch { receiveLoop(socket) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onFailure(e)
            } finally {
                socket.close()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun sendLoop(socket: DatagramSocket) {
        var sequence = 0
        mic.frames().collect { frame ->
            val pcmLength = if (isMuted()) 0 else frame.size
            val data = ByteArray(HEADER_BYTES + pcmLength)
            data[0] = (sequence shr 8).toByte()
            data[1] = sequence.toByte()
            frame.copyInto(data, HEADER_BYTES, 0, pcmLength)
            try {
                socket.send(DatagramPacket(data, data.size, remote, NetworkConfig.AUDIO_PORT))
            } catch (_: IOException) {
                // Brief network hiccup: drop the frame; the watchdog handles a real outage.
            }
            sequence = (sequence + 1) and 0xFFFF
        }
    }

    private suspend fun receiveLoop(socket: DatagramSocket) {
        val player = SpeakerPlayer().also { it.start() }
        val buffer = ByteArray(HEADER_BYTES + AudioConfig.FRAME_BYTES * 2)
        val packet = DatagramPacket(buffer, buffer.size)
        var lastSequence = -1
        try {
            while (true) {
                currentCoroutineContext().ensureActive()
                packet.length = buffer.size
                try {
                    socket.receive(packet)
                } catch (_: SocketTimeoutException) {
                    continue
                }
                if (packet.address != remote) continue
                lastHeardAt = SystemClock.elapsedRealtime()

                val pcmLength = packet.length - HEADER_BYTES
                if (pcmLength <= 0) continue
                val sequence = ((buffer[0].toInt() and 0xFF) shl 8) or (buffer[1].toInt() and 0xFF)
                if (isStale(sequence, lastSequence)) continue
                lastSequence = sequence
                player.write(buffer, HEADER_BYTES, pcmLength)
            }
        } finally {
            player.release()
        }
    }

    /** True for duplicates and packets that arrived after a newer one (16-bit wraparound aware). */
    private fun isStale(sequence: Int, last: Int): Boolean {
        if (last < 0) return false
        val diff = (sequence - last) and 0xFFFF
        return diff == 0 || diff > 0x8000
    }

    private companion object {
        const val HEADER_BYTES = 2
        const val RECEIVE_TIMEOUT_MS = 200
    }
}
