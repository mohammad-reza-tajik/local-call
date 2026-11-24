package com.localcall.network

import android.content.Context
import android.net.wifi.WifiManager

/**
 * Keeps the Wi-Fi radio awake and lets broadcast packets through while the app is talking to the
 * network. Some phones silently drop broadcasts without the multicast lock.
 */
@Suppress("DEPRECATION")
class WifiLocks(context: Context) {

    private val wifi = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val multicast = wifi.createMulticastLock("localcall:multicast")
        .apply { setReferenceCounted(false) }

    private val radio = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "localcall:radio")
        .apply { setReferenceCounted(false) }

    fun acquire() {
        if (!multicast.isHeld) multicast.acquire()
        if (!radio.isHeld) radio.acquire()
    }

    fun release() {
        if (multicast.isHeld) multicast.release()
        if (radio.isHeld) radio.release()
    }
}
