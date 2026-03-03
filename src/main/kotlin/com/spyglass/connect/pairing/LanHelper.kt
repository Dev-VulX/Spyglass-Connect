package com.spyglass.connect.pairing

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Detect the local LAN IP address for binding the WebSocket server.
 */
object LanHelper {

    /**
     * Find the best LAN IPv4 address, filtering out loopback and virtual interfaces.
     */
    fun detectLanIp(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return "127.0.0.1"

            // Priority: physical interfaces first, then virtual
            val candidates = interfaces
                .filter { !it.isLoopback && it.isUp && !it.isVirtual }
                .flatMap { iface ->
                    iface.inetAddresses.toList()
                        .filterIsInstance<Inet4Address>()
                        .filter { !it.isLoopbackAddress }
                        .map { it.hostAddress to iface.name }
                }

            // Prefer common LAN interface names
            val preferred = candidates.firstOrNull { (_, name) ->
                name.startsWith("eth") || name.startsWith("en") ||
                    name.startsWith("wlan") || name.startsWith("wl") ||
                    name.startsWith("Wi-Fi")
            }

            return preferred?.first
                ?: candidates.firstOrNull()?.first
                ?: "127.0.0.1"
        } catch (_: Exception) {
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
