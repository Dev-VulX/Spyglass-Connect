package com.spyglass.connect.minecraft

import com.spyglass.connect.Log
import com.spyglass.connect.model.PetData
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.IntArrayTag
import net.querz.nbt.tag.ListTag
import java.io.File

/**
 * Scan entity data in a Minecraft world for tamed mobs (pets).
 *
 * 1.17+: entities stored in separate entities region files.
 * Legacy: entities stored in chunk data within region files.
 */
object EntityScanner {

    private const val TAG = "EntityScanner"

    private val TAMEABLE_TYPES = setOf(
        "minecraft:wolf",
        "minecraft:cat",
        "minecraft:parrot",
        "minecraft:horse",
        "minecraft:donkey",
        "minecraft:mule",
        "minecraft:llama",
        "minecraft:trader_llama",
        "minecraft:fox",
        "minecraft:axolotl",
    )

    /** Scan all dimensions for tamed mobs. */
    fun scanWorld(worldDir: File): List<PetData> {
        val pets = mutableListOf<PetData>()
        for (dimension in listOf("overworld", "the_nether", "the_end")) {
            pets.addAll(scanDimension(worldDir, dimension))
        }
        Log.i(TAG, "Found ${pets.size} tamed mobs in ${worldDir.name}")
        return pets
    }

    private fun scanDimension(worldDir: File, dimension: String): List<PetData> {
        val pets = mutableListOf<PetData>()

        // Try 1.17+ entity files first
        val entityDir = entityRegionDir(worldDir, dimension)
        if (entityDir.isDirectory) {
            val regionFiles = entityDir.listFiles { f -> f.extension == "mca" }?.toList() ?: emptyList()
            for (regionFile in regionFiles) {
                val chunks = AnvilReader.readRegionChunks(regionFile)
                for (chunk in chunks) {
                    val entities = extractEntities(chunk) ?: continue
                    pets.addAll(parseTamedEntities(entities, dimension, worldDir))
                }
            }
        }

        // Fallback: legacy format — entities in region chunks
        if (pets.isEmpty()) {
            val regionFiles = AnvilReader.regionFiles(worldDir, dimension)
            for (regionFile in regionFiles) {
                val chunks = AnvilReader.readRegionChunks(regionFile)
                for (chunk in chunks) {
                    val entities = extractLegacyEntities(chunk) ?: continue
                    pets.addAll(parseTamedEntities(entities, dimension, worldDir))
                }
            }
        }

        return pets
    }

    private fun entityRegionDir(worldDir: File, dimension: String): File = when (dimension) {
        "overworld" -> File(worldDir, "entities")
        "the_nether" -> File(worldDir, "DIM-1/entities")
        "the_end" -> File(worldDir, "DIM1/entities")
        else -> File(worldDir, "entities")
    }

    /** Extract entities from 1.17+ entity region chunk (top-level "Entities" list). */
    @Suppress("UNCHECKED_CAST")
    private fun extractEntities(chunk: CompoundTag): ListTag<CompoundTag>? {
        return chunk.get("Entities") as? ListTag<CompoundTag>
    }

    /** Extract entities from legacy chunk format (Level.Entities or Entities at root). */
    @Suppress("UNCHECKED_CAST")
    private fun extractLegacyEntities(chunk: CompoundTag): ListTag<CompoundTag>? {
        (chunk.get("Entities") as? ListTag<CompoundTag>)?.let { return it }
        val level = NbtHelper.compound(chunk, "Level")
        return level?.get("Entities") as? ListTag<CompoundTag>
    }

    private fun parseTamedEntities(
        entities: ListTag<CompoundTag>,
        dimension: String,
        worldDir: File,
    ): List<PetData> {
        val result = mutableListOf<PetData>()
        for (i in 0 until entities.size()) {
            val entity = entities[i]
            val id = NbtHelper.string(entity, "id")
            if (id !in TAMEABLE_TYPES) continue

            // Check if tamed
            val ownerUuid = extractOwnerUuid(entity) ?: continue

            val pos = extractEntityPosition(entity)
            val customName = extractEntityCustomName(entity)
            val health = NbtHelper.float(entity, "Health")
            val maxHealth = extractMaxHealth(entity)
            val ownerName = PlayerParser.resolvePlayerName(ownerUuid, worldDir)

            val pet = PetData(
                entityType = id.removePrefix("minecraft:"),
                customName = customName,
                health = health,
                maxHealth = maxHealth,
                posX = pos.first,
                posY = pos.second,
                posZ = pos.third,
                dimension = dimension,
                ownerUuid = ownerUuid,
                ownerName = ownerName,
                collarColor = extractCollarColor(entity, id),
                catVariant = extractCatVariant(entity, id),
                horseSpeed = extractHorseAttribute(entity, "minecraft:generic.movement_speed"),
                horseJump = extractHorseAttribute(entity, "minecraft:horse.jump_strength"),
            )
            result.add(pet)
        }
        return result
    }

    private fun extractOwnerUuid(entity: CompoundTag): String? {
        // UUID int array format (modern)
        (entity.get("Owner") as? IntArrayTag)?.let { tag ->
            val ints = tag.value
            if (ints.size == 4) {
                val most = (ints[0].toLong() shl 32) or (ints[1].toLong() and 0xFFFFFFFFL)
                val least = (ints[2].toLong() shl 32) or (ints[3].toLong() and 0xFFFFFFFFL)
                return java.util.UUID(most, least).toString()
            }
        }
        // String format
        val ownerStr = NbtHelper.string(entity, "Owner")
        if (ownerStr.isNotBlank() && ownerStr.contains("-")) return ownerStr
        // OwnerUUID string (older)
        val ownerUuidStr = NbtHelper.string(entity, "OwnerUUID")
        if (ownerUuidStr.isNotBlank() && ownerUuidStr.contains("-")) return ownerUuidStr
        return null
    }

    private fun extractEntityPosition(entity: CompoundTag): Triple<Double, Double, Double> {
        @Suppress("UNCHECKED_CAST")
        val posList = entity.get("Pos") as? ListTag<*> ?: return Triple(0.0, 0.0, 0.0)
        if (posList.size() < 3) return Triple(0.0, 0.0, 0.0)
        return Triple(
            posList[0].valueToString().toDoubleOrNull() ?: 0.0,
            posList[1].valueToString().toDoubleOrNull() ?: 0.0,
            posList[2].valueToString().toDoubleOrNull() ?: 0.0,
        )
    }

    private fun extractEntityCustomName(entity: CompoundTag): String? {
        val raw = NbtHelper.string(entity, "CustomName")
        if (raw.isBlank()) return null
        if (raw.startsWith("{") || raw.startsWith("\"")) {
            return try {
                val textMatch = Regex(""""text"\s*:\s*"([^"]*?)"""").find(raw)
                textMatch?.groupValues?.get(1)?.ifBlank { null } ?: raw.trim('"')
            } catch (_: Exception) {
                raw.trim('"')
            }
        }
        return raw
    }

    private fun extractMaxHealth(entity: CompoundTag): Float {
        @Suppress("UNCHECKED_CAST")
        val attributes = (entity.get("Attributes") as? ListTag<CompoundTag>) ?: return 20f
        for (i in 0 until attributes.size()) {
            val attr = attributes[i]
            val name = NbtHelper.string(attr, "Name")
            if (name == "minecraft:generic.max_health" || name == "generic.max_health") {
                return NbtHelper.double(attr, "Base", 20.0).toFloat()
            }
        }
        return 20f
    }

    private fun extractCollarColor(entity: CompoundTag, id: String): Int {
        if (id != "minecraft:wolf" && id != "minecraft:cat") return -1
        return NbtHelper.int(entity, "CollarColor", -1)
    }

    private fun extractCatVariant(entity: CompoundTag, id: String): String? {
        if (id != "minecraft:cat") return null
        // 1.19+: variant string
        val variant = NbtHelper.string(entity, "variant")
        if (variant.isNotBlank()) return variant.removePrefix("minecraft:")
        // Legacy: CatType int
        val catType = NbtHelper.int(entity, "CatType", -1)
        if (catType >= 0) return CAT_TYPE_MAP[catType]
        return null
    }

    private fun extractHorseAttribute(entity: CompoundTag, attrName: String): Double {
        @Suppress("UNCHECKED_CAST")
        val attributes = (entity.get("Attributes") as? ListTag<CompoundTag>) ?: return 0.0
        // Try both namespaced and plain variants
        val plain = attrName.removePrefix("minecraft:")
        for (i in 0 until attributes.size()) {
            val attr = attributes[i]
            val name = NbtHelper.string(attr, "Name")
            if (name == attrName || name == plain) {
                return NbtHelper.double(attr, "Base")
            }
        }
        return 0.0
    }

    private val CAT_TYPE_MAP = mapOf(
        0 to "tabby", 1 to "black", 2 to "red", 3 to "siamese",
        4 to "british_shorthair", 5 to "calico", 6 to "persian",
        7 to "ragdoll", 8 to "white", 9 to "jellie", 10 to "all_black",
    )
}
