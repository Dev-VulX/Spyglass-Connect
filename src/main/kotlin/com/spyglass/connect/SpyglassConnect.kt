package com.spyglass.connect

import androidx.compose.runtime.*
import androidx.compose.ui.window.application
import com.spyglass.connect.minecraft.SaveDetector
import com.spyglass.connect.model.WorldInfo
import com.spyglass.connect.pairing.LanHelper
import com.spyglass.connect.pairing.MdnsPublisher
import com.spyglass.connect.server.WebSocketServer
import com.spyglass.connect.ui.MainWindow
import com.spyglass.connect.ui.SystemTray
import com.spyglass.connect.watcher.WorldWatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun main() {
    val server = WebSocketServer()
    val mdns = MdnsPublisher()
    val watcher = arrayOfNulls<WorldWatcher>(1) // holder for shutdown hook access

    // Ensure cleanup on any exit (window close, kill signal, etc.)
    Runtime.getRuntime().addShutdownHook(Thread {
        watcher[0]?.stop()
        mdns.stop()
        server.stop()
    })

    application {
        val scope = rememberCoroutineScope()
        val lanIp = remember { LanHelper.detectLanIp() }
        val worlds = remember { mutableStateListOf<WorldInfo>() }
        var worldsLoaded by remember { mutableStateOf(false) }
        var refreshTrigger by remember { mutableStateOf(0) }

        val worldWatcher = remember {
            WorldWatcher(scope) { categories ->
                server.invalidateCache()
                server.notifyWorldChanged("", categories.toList())
            }.also { watcher[0] = it }
        }

        // Detect Minecraft worlds on startup and on config change
        LaunchedEffect(refreshTrigger) {
            worldsLoaded = false
            withContext(Dispatchers.IO) {
                val detected = SaveDetector.detectWorlds()
                worlds.clear()
                worlds.addAll(detected)
            }
            worldsLoaded = true
        }

        // Start WebSocket server + mDNS
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                server.start()
                mdns.start(WebSocketServer.DEFAULT_PORT, lanIp)
            }
        }

        // Watch first world's directory for changes
        LaunchedEffect(worlds.firstOrNull()) {
            val firstWorld = worlds.firstOrNull() ?: return@LaunchedEffect
            val worldPath = firstWorld.sourcePath
            if (worldPath.isNotEmpty()) {
                val worldDir = java.io.File(worldPath)
                if (worldDir.isDirectory) {
                    worldWatcher.watch(worldDir)
                }
            }
        }

        val shutdown: () -> Unit = {
            watcher[0]?.stop()
            mdns.stop()
            server.stop()
            exitApplication()
        }

        SystemTray(
            serverState = server.state,
            onShowWindow = { /* handled by window visibility */ },
            onQuit = shutdown,
        )

        MainWindow(
            worlds = worlds,
            worldsLoaded = worldsLoaded,
            serverState = server.state,
            connectedDevices = server.connectedDevices,
            lanIp = lanIp,
            serverPort = WebSocketServer.DEFAULT_PORT,
            onRefreshWorlds = { refreshTrigger++ },
            onCloseRequest = shutdown,
        )
    }
}
