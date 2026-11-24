package com.localcall.call

import java.net.InetAddress

/** The local user, as announced to others on the network. */
data class Identity(val id: String, val name: String)

/** Another device discovered on the local network. */
data class Peer(
    val id: String,
    val name: String,
    val address: InetAddress,
    val lastSeen: Long,
) {
    val host: String get() = address.hostAddress.orEmpty()
}
