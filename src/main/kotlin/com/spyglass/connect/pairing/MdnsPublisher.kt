package com.spyglass.connect.pairing

import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import java.net.InetAddress

/**
 * Publish mDNS service (_spyglass._tcp) on the local network
 * so Android clients can discover the desktop app for reconnection.
 */
class MdnsPublisher {

    companion object {
        const val SERVICE_TYPE = "_spyglass._tcp.local."
        const val SERVICE_NAME = "Spyglass Connect"
    }

    private var jmdns: JmDNS? = null
    private var serviceInfo: ServiceInfo? = null

    /** Start publishing the mDNS service. */
    fun start(port: Int, ip: String = LanHelper.detectLanIp()) {
        try {
            val address = InetAddress.getByName(ip)
            jmdns = JmDNS.create(address, "spyglass-connect")

            serviceInfo = ServiceInfo.create(
                SERVICE_TYPE,
                SERVICE_NAME,
                port,
                "Spyglass Connect Desktop Companion",
            )

            jmdns?.registerService(serviceInfo)
        } catch (e: Exception) {
            // mDNS not critical — clients can fall back to manual IP
        }
    }

    /** Stop publishing. */
    fun stop() {
        try {
            serviceInfo?.let { jmdns?.unregisterService(it) }
            jmdns?.close()
        } catch (_: Exception) {
            // Ignore cleanup errors
        } finally {
            jmdns = null
            serviceInfo = null
        }
    }
}
