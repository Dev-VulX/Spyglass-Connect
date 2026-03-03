package com.spyglass.connect.pairing

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persist paired device keys and info to ~/.spyglass-connect/devices.json
 * for reconnection without re-scanning QR.
 */
object DeviceStore {

    @Serializable
    data class PairedDevice(
        val id: String,
        val deviceName: String,
        val publicKey: String,
        val lastConnected: Long = System.currentTimeMillis(),
    )

    @Serializable
    private data class DeviceFile(
        val devices: MutableList<PairedDevice> = mutableListOf(),
    )

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val configDir = File(System.getProperty("user.home"), ".spyglass-connect")
    private val devicesFile = File(configDir, "devices.json")

    /** Save or update a paired device. */
    fun savePairedDevice(device: PairedDevice) {
        val data = load()
        data.devices.removeAll { it.id == device.id }
        data.devices.add(device)
        save(data)
    }

    /** Get all paired devices. */
    fun getPairedDevices(): List<PairedDevice> = load().devices

    /** Remove a paired device by ID. */
    fun removePairedDevice(id: String) {
        val data = load()
        data.devices.removeAll { it.id == id }
        save(data)
    }

    /** Check if a public key belongs to a known paired device. */
    fun findByPublicKey(publicKey: String): PairedDevice? {
        return load().devices.firstOrNull { it.publicKey == publicKey }
    }

    private fun load(): DeviceFile {
        if (!devicesFile.exists()) return DeviceFile()
        return try {
            json.decodeFromString(DeviceFile.serializer(), devicesFile.readText())
        } catch (_: Exception) {
            DeviceFile()
        }
    }

    private fun save(data: DeviceFile) {
        configDir.mkdirs()
        devicesFile.writeText(json.encodeToString(data))
    }
}
