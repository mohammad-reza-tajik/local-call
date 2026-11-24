package com.localcall.network

import java.net.InetAddress
import java.net.NetworkInterface

object NetworkUtils {

    /**
     * Every IPv4 broadcast address reachable from this device. Walking the interfaces (instead of
     * only using 255.255.255.255) makes discovery work both on a normal Wi-Fi network and when
     * this phone is the hotspot host.
     */
    fun broadcastAddresses(): List<InetAddress> {
        val result = linkedSetOf<InetAddress>()
        try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.interfaceAddresses }
                .mapNotNullTo(result) { it.broadcast }
        } catch (_: Exception) {
            // Fall through to the limited broadcast address below.
        }
        result += InetAddress.getByName("255.255.255.255")
        return result.toList()
    }
}
