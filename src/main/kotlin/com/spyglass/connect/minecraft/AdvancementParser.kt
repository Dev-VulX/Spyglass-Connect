package com.spyglass.connect.minecraft

import com.spyglass.connect.model.AdvancementStatus
import kotlinx.serialization.json.*
import java.io.File

/**
 * Parse player advancements from saves/<world>/advancements/<uuid>.json.
 *
 * Minecraft advancements JSON structure:
 * ```json
 * {
 *   "minecraft:story/root": {
 *     "criteria": { "in_game": "2024-01-01 12:00:00 -0600" },
 *     "done": true
 *   },
 *   "DataVersion": 3700
 * }
 * ```
 */
object AdvancementParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(worldDir: File, playerUuid: String): List<AdvancementStatus> {
        val advFile = File(worldDir, "advancements/$playerUuid.json")
        if (!advFile.exists()) return emptyList()

        val root = try {
            json.parseToJsonElement(advFile.readText()).jsonObject
        } catch (_: Exception) {
            return emptyList()
        }

        return root.entries.mapNotNull { (key, value) ->
            // Skip non-advancement entries like DataVersion
            if (!key.contains(":")) return@mapNotNull null
            val obj = value.jsonObject

            val done = obj["done"]?.jsonPrimitive?.booleanOrNull ?: false
            val criteria = obj["criteria"]?.jsonObject?.entries?.associate { (k, v) ->
                k to (v.jsonPrimitive.contentOrNull ?: "")
            } ?: emptyMap()

            AdvancementStatus(
                id = key.removePrefix("minecraft:"),
                done = done,
                criteria = criteria,
            )
        }
    }
}
