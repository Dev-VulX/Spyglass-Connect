package com.spyglass.connect.pairing

import com.spyglass.connect.Log
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Detect the local LAN IP address for binding the WebSocket server.
 */
object LanHelper {

    private const val TAG = "LAN"

    /**
     * Find the best LAN IPv4 address, filtering out loopback and virtual interfaces.
     */
    fun detectLanIp(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList()
            if (interfaces == null) {
                Log.w(TAG, "No network interfaces found")
                return "127.0.0.1"
            }

            // Priority: physical interfaces first, then virtual
            val candidates = interfaces
                .filter { !it.isLoopback && it.isUp && !it.isVirtual }
                .flatMap { iface ->
                    iface.inetAddresses.toList()
                        .filterIsInstance<Inet4Address>()
                        .filter { !it.isLoopbackAddress }
                        .map { it.hostAddress to iface.name }
                }

            Log.d(TAG, "Network candidates: ${candidates.joinToString { "${it.first} (${it.second})" }.ifEmpty { "none" }}")

            // Prefer common LAN interface names
            val preferred = candidates.firstOrNull { (_, name) ->
                name.startsWith("eth") || name.startsWith("en") ||
                    name.startsWith("wlan") || name.startsWith("wl") ||
                    name.startsWith("Wi-Fi")
            }

            val result = preferred?.first
                ?: candidates.firstOrNull()?.first
                ?: "127.0.0.1"

            Log.i(TAG, "Detected LAN IP: $result")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect LAN IP", e)
            return "127.0.0.1"
        }
    }

    /** Check if an IP address is in a private range. */
    fun isPrivateIp(ip: String): Boolean {
        val parts = ip.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false
        return when {
            parts[0] == 10 -> true
            parts[0] == 172 && parts[1] in 16..31 -> true
            parts[0] == 192 && parts[1] == 168 -> true
            else -> false
        }
    }
}
