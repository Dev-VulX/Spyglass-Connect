package com.spyglass.connect.server

import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Track connected WebSocket clients and their encryption state.
 */
class SessionManager {

    data class ClientSession(
        val id: String,
        val session: WebSocketSession,
        val encryption: EncryptionManager,
        val deviceName: String = "Unknown",
        var isPaired: Boolean = false,
    )

    private val mutex = Mutex()
    private val sessions = mutableMapOf<String, ClientSession>()

    /** Register a new client session. */
    suspend fun addSession(id: String, session: WebSocketSession, encryption: EncryptionManager): ClientSession {
        val clientSession = ClientSession(id = id, session = session, encryption = encryption)
        mutex.withLock {
            sessions[id] = clientSession
        }
        return clientSession
    }

    /** Remove a client session. */
    suspend fun removeSession(id: String) {
        mutex.withLock {
            sessions.remove(id)
        }
    }

    /** Get all active sessions. */
    suspend fun activeSessions(): List<ClientSession> {
        return mutex.withLock { sessions.values.toList() }
    }

    /** Get a specific session. */
    suspend fun getSession(id: String): ClientSession? {
        return mutex.withLock { sessions[id] }
    }

    /** Mark a session as paired. */
    suspend fun markPaired(id: String, deviceName: String) {
        mutex.withLock {
            sessions[id]?.let {
                sessions[id] = it.copy(isPaired = true, deviceName = deviceName)
            }
        }
    }

    /** Get count of connected clients. */
    suspend fun connectionCount(): Int = mutex.withLock { sessions.size }

    /** Send an encrypted message to all paired sessions. */
    suspend fun broadcast(message: String) {
        val active = activeSessions().filter { it.isPaired }
        for (client in active) {
            try {
                if (client.encryption.isReady) {
                    client.session.send(Frame.Text(client.encryption.encrypt(message)))
                } else {
                    client.session.send(Frame.Text(message))
                }
            } catch (_: Exception) {
                removeSession(client.id)
            }
        }
    }
}
