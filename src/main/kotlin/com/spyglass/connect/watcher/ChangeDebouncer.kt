package com.spyglass.connect.watcher

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coalesce rapid file system changes (500ms window) into single re-scan events.
 * Minecraft writes to multiple files rapidly when saving; we don't want to
 * trigger multiple scans for a single save event.
 */
class ChangeDebouncer(
    private val scope: CoroutineScope,
    private val delayMs: Long = 500L,
    private val onDebounced: suspend (Set<String>) -> Unit,
) {

    private val mutex = Mutex()
    private val pendingCategories = mutableSetOf<String>()
    private var debounceJob: Job? = null

    /** Record a change in a category (e.g., "player", "region", "level"). */
    suspend fun onChange(category: String) {
        mutex.withLock {
            pendingCategories.add(category)
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(delayMs)
                flush()
            }
        }
    }

    /** Flush pending changes. */
    private suspend fun flush() {
        val categories = mutex.withLock {
            val copy = pendingCategories.toSet()
            pendingCategories.clear()
            copy
        }
        if (categories.isNotEmpty()) {
            onDebounced(categories)
        }
    }
}
