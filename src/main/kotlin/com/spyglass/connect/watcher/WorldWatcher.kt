package com.spyglass.connect.watcher

import com.spyglass.connect.Log
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.*

/**
 * Watch a Minecraft world folder for changes using Java NIO WatchService.
 * Monitors: level.dat, playerdata/, region/, DIM-1/region/, DIM1/region/
 *
 * Inspired by minecolony-manager/electron/main.js polling pattern,
 * but using native filesystem events instead.
 */
class WorldWatcher(
    private val scope: CoroutineScope,
    private val onChanged: suspend (Set<String>) -> Unit,
) {

    private var watchJob: Job? = null
    private var watchService: WatchService? = null

    companion object {
        private const val TAG = "Watcher"
    }

    /** Start watching a world directory. Cancels any previous watch. */
    fun watch(worldDir: File) {
        stop()
        Log.i(TAG, "Watching ${worldDir.absolutePath}")

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

            val debouncer = ChangeDebouncer(this, 500L, onChanged)
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
    }

    /** Stop watching. */
    fun stop() {
        watchJob?.cancel()
        watchJob = null
        try { watchService?.close() } catch (_: Exception) {}
        watchService = null
    }
}
