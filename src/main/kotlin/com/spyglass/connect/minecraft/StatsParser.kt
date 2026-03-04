package com.spyglass.connect.minecraft

import com.spyglass.connect.model.StatCategory
import com.spyglass.connect.model.StatEntry
import kotlinx.serialization.json.*
import java.io.File

/**
 * Parse player statistics from saves/<world>/stats/<uuid>.json.
 *
 * Minecraft stats JSON structure:
 * ```json
 * {
 *   "stats": {
 *     "minecraft:mined": { "minecraft:stone": 1234, ... },
 *     "minecraft:crafted": { ... },
 *     ...
 *   },
 *   "DataVersion": 3700
 * }
 * ```
 */
object StatsParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(worldDir: File, playerUuid: String): List<StatCategory> {
        val statsFile = File(worldDir, "stats/$playerUuid.json")
        if (!statsFile.exists()) return emptyList()

        val root = try {
            json.parseToJsonElement(statsFile.readText()).jsonObject
        } catch (_: Exception) {
            return emptyList()
        }

        val statsObj = root["stats"]?.jsonObject ?: return emptyList()

        return statsObj.entries.mapNotNull { (categoryKey, categoryValue) ->
            val entries = categoryValue.jsonObject.entries.map { (statKey, statValue) ->
                StatEntry(
                    key = statKey.removePrefix("minecraft:"),
                    value = statValue.jsonPrimitive.long,
                )
            }.sortedByDescending { it.value }

            if (entries.isEmpty()) return@mapNotNull null

            StatCategory(
                category = categoryKey.removePrefix("minecraft:"),
                entries = entries,
            )
        }.sortedBy { it.category }
    }
}
