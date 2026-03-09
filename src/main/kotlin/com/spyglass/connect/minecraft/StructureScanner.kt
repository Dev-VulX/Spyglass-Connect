package com.spyglass.connect.minecraft

import com.spyglass.connect.model.StructureLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import net.querz.nbt.tag.CompoundTag
import java.io.File

/**
 * Extract structure locations from Minecraft world data.
 * Reads structure starts from chunk NBT (1.18+ format: "structures.starts")
 * and falls back to data .dat files for older formats.
 */
object StructureScanner {

    /** Known vanilla structure types. */
    private val VANILLA_STRUCTURES = setOf(
        "minecraft:village", "minecraft:desert_pyramid", "minecraft:jungle_pyramid",
        "minecraft:swamp_hut", "minecraft:igloo", "minecraft:ocean_monument",
        "minecraft:woodland_mansion", "minecraft:stronghold", "minecraft:mineshaft",
        "minecraft:ocean_ruin", "minecraft:shipwreck", "minecraft:buried_treasure",
        "minecraft:pillager_outpost", "minecraft:ruined_portal", "minecraft:bastion_remnant",
        "minecraft:fortress", "minecraft:nether_fossil", "minecraft:end_city",
        "minecraft:ancient_city", "minecraft:trail_ruins", "minecraft:trial_chambers",
    )

    /**
     * Scan all structures in a world across all dimensions.
     */
    fun scanWorld(worldDir: File): List<StructureLocation> {
        val seen = mutableSetOf<String>() // Deduplicate by "type:x:z"

        val allRegionWork = listOf("overworld", "the_nether", "the_end").flatMap { dimension ->
            AnvilReader.regionFiles(worldDir, dimension).map { it to dimension }
        }

        val allLocations = if (allRegionWork.isEmpty()) emptyList() else runBlocking(Dispatchers.IO) {
            allRegionWork.map { (regionFile, dimension) ->
                async {
                    val locs = mutableListOf<StructureLocation>()
                    val chunks = AnvilReader.readRegionChunks(regionFile)
                    for (chunk in chunks) {
                        locs.addAll(extractFromChunk(chunk, dimension))
                    }
                    locs
                }
            }.awaitAll().flatten()
        }

        return allLocations.filter { loc ->
            seen.add("${loc.type}:${loc.x}:${loc.z}")
        }.sortedBy { it.type }
    }

    /** Extract structure starts from a single chunk NBT. */
    private fun extractFromChunk(chunkNbt: CompoundTag, dimension: String): List<StructureLocation> {
        val results = mutableListOf<StructureLocation>()

        // 1.18+ format: "structures" → "starts"
        val structures = NbtHelper.compound(chunkNbt, "structures")
            ?: NbtHelper.compound(chunkNbt, "Level", "Structures")

        val starts = NbtHelper.compound(structures ?: return results, "starts")
            ?: NbtHelper.compound(structures, "Starts")
            ?: return results

        for (key in starts.keySet()) {
            val start = starts.get(key) as? CompoundTag ?: continue
            val id = NbtHelper.string(start, "id")
            if (id.isBlank() || id == "INVALID") continue

            // BB (bounding box) array: [minX, minY, minZ, maxX, maxY, maxZ]
            val bb = start.getIntArray("BB")
            val (x, y, z) = if (bb != null && bb.size >= 6) {
                Triple((bb[0] + bb[3]) / 2, bb[1], (bb[2] + bb[5]) / 2)
            } else {
                // Fall back to chunk position
                val cx = NbtHelper.int(start, "ChunkX") * 16 + 8
                val cz = NbtHelper.int(start, "ChunkZ") * 16 + 8
                Triple(cx, 64, cz)
            }

            results.add(
                StructureLocation(
                    type = id.removePrefix("minecraft:"),
                    x = x, y = y, z = z,
                    dimension = dimension,
                )
            )
        }

        return results
    }
}
