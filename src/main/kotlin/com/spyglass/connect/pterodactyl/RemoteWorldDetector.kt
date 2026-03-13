package com.spyglass.connect.pterodactyl

import com.spyglass.connect.Log
import com.spyglass.connect.minecraft.SaveDetector
import com.spyglass.connect.model.WorldInfo
import java.io.File

/**
 * Discover Minecraft worlds on a Pterodactyl server by scanning for level.dat files
 * in common server directory structures.
 *
 * Handles Paper/Spigot's split-dimension layout where nether and end are stored as
 * separate top-level directories (world_nether, world_the_end) rather than vanilla's
 * nested layout (world/DIM-1, world/DIM1). These are merged into a single world entry.
 */
object RemoteWorldDetector {

    private const val TAG = "PteroDetect"

    /** Suffixes Paper/Spigot append to the main world name for other dimensions. */
    private val PAPER_DIMENSION_SUFFIXES = listOf("_nether", "_the_end")

    /**
     * Detect worlds on a remote Pterodactyl server.
     * Downloads level.dat files to parse world metadata, then returns WorldInfo objects.
     */
    suspend fun detectWorlds(
        client: PterodactylClient,
        serverId: String,
        serverName: String,
        cache: RemoteWorldCache,
    ): List<WorldInfo> {
        val worlds = mutableListOf<WorldInfo>()

        try {
            // First, list root directory to find world folders
            val rootFiles = client.listFiles(serverId, "/")
            val rootDirs = rootFiles.filter { !it.isFile }.map { it.name }
            val rootHasLevelDat = rootFiles.any { it.isFile && it.name == "level.dat" }

            Log.d(TAG, "Root dirs: $rootDirs, has level.dat: $rootHasLevelDat")

            // Detect Paper/Spigot split-dimension directories to exclude them
            val paperDimensionDirs = mutableSetOf<String>()

            // Check each candidate location
            val candidates = mutableListOf<String>()

            // Standard "world" directory
            if ("world" in rootDirs) {
                candidates.add("/world")
                // Check for Paper's split dimensions (world_nether, world_the_end)
                for (suffix in PAPER_DIMENSION_SUFFIXES) {
                    if ("world$suffix" in rootDirs) {
                        paperDimensionDirs.add("world$suffix")
                        Log.d(TAG, "Detected Paper dimension dir: world$suffix (will merge into /world)")
                    }
                }
            }

            // Check for level.dat in root (some server setups)
            if (rootHasLevelDat) {
                candidates.add("/")
            }

            // Check other common names (skip known non-world dirs and Paper dimension dirs)
            val skipDirs = setOf("world", "plugins", "config", "logs", "cache", "libraries", "versions") + paperDimensionDirs
            for (name in rootDirs) {
                if (name in skipDirs) continue
                // Check if this directory has a level.dat
                try {
                    val dirFiles = client.listFiles(serverId, "/$name")
                    if (dirFiles.any { it.isFile && it.name == "level.dat" }) {
                        candidates.add("/$name")
                        // Also check for Paper dimension dirs for this world name
                        for (suffix in PAPER_DIMENSION_SUFFIXES) {
                            if ("$name$suffix" in rootDirs) {
                                paperDimensionDirs.add("$name$suffix")
                                Log.d(TAG, "Detected Paper dimension dir: $name$suffix (will merge into /$name)")
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Skip directories we can't read
                }
            }

            Log.i(TAG, "World candidates for '$serverName': $candidates")
            if (paperDimensionDirs.isNotEmpty()) {
                Log.i(TAG, "Paper dimension dirs (merged): $paperDimensionDirs")
            }

            // Download and parse level.dat for each candidate
            for (worldPath in candidates) {
                try {
                    val levelDatPath = if (worldPath == "/") "/level.dat" else "$worldPath/level.dat"
                    val levelDatBytes = client.downloadFile(serverId, levelDatPath)

                    // Write to cache so we can use SaveDetector.parseWorldInfo()
                    val cacheDir = cache.worldCacheDir(serverId, worldPath)
                    cacheDir.mkdirs()
                    File(cacheDir, "level.dat").writeBytes(levelDatBytes)

                    // Detect if this world uses Paper's split-dimension layout
                    val worldName = worldPath.trimStart('/')
                    val isPaperLayout = PAPER_DIMENSION_SUFFIXES.any { "${worldName}${it}" in paperDimensionDirs }
                    if (isPaperLayout) {
                        cache.setPaperLayout(serverId, worldPath, true)
                    }

                    val worldInfo = SaveDetector.parseWorldInfo(cacheDir)
                    if (worldInfo != null) {
                        val folderName = if (worldPath == "/") "server_root" else worldPath.trimStart('/')
                        worlds.add(
                            worldInfo.copy(
                                folderName = "ptero_${serverId}_$folderName",
                                sourcePath = "ptero://$serverId$worldPath",
                                sourceLabel = "Ptero: $serverName",
                            )
                        )
                        Log.i(TAG, "Found world: ${worldInfo.displayName} at $worldPath (paper=$isPaperLayout)")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse world at $worldPath: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "World detection failed for server '$serverName'", e)
        }

        return worlds
    }
}
