package com.spyglass.connect.watcher

import com.spyglass.connect.Log
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.*

/**
 * Watch a Minecraft world folder for changes.
 * Uses Java NIO WatchService (inotify) for instant detection, plus a polling
 * fallback every 3 seconds for cases where inotify misses events (e.g. Flatpak
 * sandboxed games that write files atomically via rename).
 *
 * Monitors: level.dat, playerdata/, region/, DIM-1/region/, DIM1/region/
 */
class WorldWatcher(
    private val scope: CoroutineScope,
    private val onChanged: suspend (Set<String>) -> Unit,
) {

    private var watchJob: Job? = null
    private var pollJob: Job? = null
    private var watchService: WatchService? = null

    companion object {
        private const val TAG = "Watcher"
        private const val POLL_INTERVAL_MS = 3000L
    }

    /** Start watching a world directory. Cancels any previous watch. */
    fun watch(worldDir: File) {
        stop()
        Log.i(TAG, "Watching ${worldDir.absolutePath}")

        val debouncer = ChangeDebouncer(scope, 500L, onChanged)

        // inotify-based watcher (instant but may miss atomic renames)
        watchJob = scope.launch(Dispatchers.IO) {
            val ws = FileSystems.getDefault().newWatchService()
            watchService = ws

            val dirsToWatch = buildList {
                add(worldDir.toPath() to "level")
                val playerData = File(worldDir, "playerdata")
                if (playerData.isDirectory) add(playerData.toPath() to "player")
                for ((subdir, category) in listOf(
                    "region" to "region_overworld",
                    "DIM-1/region" to "region_nether",
                    "DIM1/region" to "region_end",
                )) {
                    val dir = File(worldDir, subdir)
                    if (dir.isDirectory) add(dir.toPath() to category)
                }
            }

            val keyToCategory = mutableMapOf<WatchKey, String>()

            for ((path, category) in dirsToWatch) {
                try {
                    val key = path.register(
                        ws,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE,
                    )
                    keyToCategory[key] = category
                    Log.d(TAG, "Registered watch: $path ($category)")
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot watch $path: ${e.message}")
                }
            }

            try {
                while (isActive) {
                    val key = ws.poll(1, java.util.concurrent.TimeUnit.SECONDS) ?: continue
                    val category = keyToCategory[key] ?: "unknown"

                    for (event in key.pollEvents()) {
                        val kind = event.kind()
                        if (kind == StandardWatchEventKinds.OVERFLOW) continue
                        debouncer.onChange(category)
                    }

                    if (!key.reset()) {
                        keyToCategory.remove(key)
                        if (keyToCategory.isEmpty()) break
                    }
                }
            } catch (_: ClosedWatchServiceException) {
                Log.d(TAG, "Watch service closed")
            }
        }

        // Polling fallback — catches changes inotify misses
        pollJob = scope.launch(Dispatchers.IO) {
            // Snapshot initial timestamps
            val lastModified = mutableMapOf<String, Long>()
            val trackedPaths = mutableSetOf<String>()

            fun addTrackedFile(file: File) {
                val path = file.absolutePath
                if (trackedPaths.add(path)) {
                    lastModified[path] = file.lastModified()
                }
            }

            addTrackedFile(File(worldDir, "level.dat"))
            val playerDataDir = File(worldDir, "playerdata")
            if (playerDataDir.isDirectory) {
                playerDataDir.listFiles()?.filter { it.name.endsWith(".dat") }?.forEach {
                    addTrackedFile(it)
                }
            }

            Log.d(TAG, "Poll fallback tracking ${trackedPaths.size} files")

            while (isActive) {
                delay(POLL_INTERVAL_MS)

                // Pick up new playerdata files (O(n) set lookup instead of O(n²))
                if (playerDataDir.isDirectory) {
                    playerDataDir.listFiles()?.filter { it.name.endsWith(".dat") }?.forEach { f ->
                        addTrackedFile(f)
                    }
                }

                for (path in trackedPaths) {
                    val file = File(path)
                    val mod = file.lastModified()
                    val prev = lastModified[path] ?: continue
                    if (mod > prev) {
                        lastModified[path] = mod
                        val category = if (path.contains("playerdata")) "player" else "level"
                        Log.d(TAG, "Poll detected change: ${file.name} ($category)")
                        debouncer.onChange(category)
                    }
                }
            }
        }
    }

    /** Stop watching. */
    fun stop() {
        watchJob?.cancel()
        watchJob = null
        pollJob?.cancel()
        pollJob = null
        try { watchService?.close() } catch (_: Exception) {}
        watchService = null
    }
}
