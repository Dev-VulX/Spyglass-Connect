package com.spyglass.connect.minecraft

import com.spyglass.connect.model.ItemStack
import com.spyglass.connect.model.PlayerData
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.ListTag
import java.io.File

/**
 * Parse player inventory, health, position, etc. from Minecraft save data.
 *
 * Singleplayer: inventory in level.dat → Data → Player
 * Multiplayer: inventory in playerdata/<uuid>.dat
 */
object PlayerParser {

    private val DIMENSION_MAP = mapOf(
        "minecraft:overworld" to "overworld",
        "minecraft:the_nether" to "the_nether",
        "minecraft:the_end" to "the_end",
    )

    /** Parse player data from a world directory. Tries singleplayer first, then playerdata. */
    fun parse(worldDir: File): PlayerData? {
        // Try singleplayer (level.dat → Data → Player)
        val levelDat = File(worldDir, "level.dat")
        val root = NbtHelper.readCompressed(levelDat) ?: return null
        val data = NbtHelper.compound(root, "Data") ?: return null
        val worldName = NbtHelper.string(data, "LevelName", worldDir.name)

        // Singleplayer player compound
        NbtHelper.compound(data, "Player")?.let { player ->
            return parsePlayerCompound(player, worldName)
        }

        // Fall back to playerdata directory (multiplayer or dedicated server)
        val playerDataDir = File(worldDir, "playerdata")
        if (playerDataDir.isDirectory) {
            val datFiles = playerDataDir.listFiles { f -> f.extension == "dat" }
                ?.sortedByDescending { it.lastModified() }
            datFiles?.firstOrNull()?.let { datFile ->
                val playerRoot = NbtHelper.readCompressed(datFile) ?: return null
                return parsePlayerCompound(playerRoot, worldName)
            }
        }

        return null
    }

    /** Parse a player compound tag into PlayerData. */
    private fun parsePlayerCompound(player: CompoundTag, worldName: String): PlayerData {
        val pos = extractPosition(player)
        val dimension = extractDimension(player)

        return PlayerData(
            worldName = worldName,
            health = NbtHelper.float(player, "Health", 20f),
            foodLevel = NbtHelper.int(player, "foodLevel", 20),
            xpLevel = NbtHelper.int(player, "XpLevel"),
            xpProgress = NbtHelper.float(player, "XpP"),
            posX = pos.first,
            posY = pos.second,
            posZ = pos.third,
            dimension = dimension,
            inventory = parseInventory(player),
            armor = parseArmor(player),
            offhand = parseOffhand(player),
            enderChest = parseEnderChest(player),
        )
    }

    /** Extract XYZ position from the Pos list tag. */
    private fun extractPosition(player: CompoundTag): Triple<Double, Double, Double> {
        @Suppress("UNCHECKED_CAST")
        val posList = player.get("Pos") as? ListTag<*> ?: return Triple(0.0, 0.0, 0.0)
        if (posList.size() < 3) return Triple(0.0, 0.0, 0.0)
        return Triple(
            posList[0].valueToString().toDoubleOrNull() ?: 0.0,
            posList[1].valueToString().toDoubleOrNull() ?: 0.0,
            posList[2].valueToString().toDoubleOrNull() ?: 0.0,
        )
    }

    /** Extract dimension string. Handles both old int and new string formats. */
    private fun extractDimension(player: CompoundTag): String {
        val dim = NbtHelper.string(player, "Dimension", "")
        if (dim.isNotEmpty()) return DIMENSION_MAP[dim] ?: dim.removePrefix("minecraft:")
        // Old format (pre-1.16): integer dimension
        return when (NbtHelper.int(player, "Dimension", 0)) {
            -1 -> "the_nether"
            1 -> "the_end"
            else -> "overworld"
        }
    }

    /** Parse main inventory (slots 0-35). */
    private fun parseInventory(player: CompoundTag): List<ItemStack> {
        return parseItemList(player, "Inventory") { slot -> slot in 0..35 }
    }

    /** Parse armor slots (100-103). */
    private fun parseArmor(player: CompoundTag): List<ItemStack> {
        return parseItemList(player, "Inventory") { slot -> slot in 100..103 }
    }

    /** Parse offhand (slot -106). */
    private fun parseOffhand(player: CompoundTag): ItemStack? {
        return parseItemList(player, "Inventory") { slot -> slot == -106 }.firstOrNull()
    }

    /** Parse ender chest contents. */
    private fun parseEnderChest(player: CompoundTag): List<ItemStack> {
        return parseItemList(player, "EnderItems") { true }
    }

    /** Generic item list parser with slot filter. */
    private fun parseItemList(
        parent: CompoundTag,
        listKey: String,
        slotFilter: (Int) -> Boolean,
    ): List<ItemStack> {
        @Suppress("UNCHECKED_CAST")
        val items = parent.get(listKey) as? ListTag<CompoundTag> ?: return emptyList()
        return items.mapNotNull { item -> parseItem(item) }
            .filter { slotFilter(it.slot) }
    }

    /** Parse a single item compound into ItemStack. */
    private fun parseItem(item: CompoundTag): ItemStack? {
        val id = NbtHelper.string(item, "id").removePrefix("minecraft:")
        if (id.isBlank()) return null
        val count = NbtHelper.int(item, "Count", 1).coerceAtLeast(1)
        // Slot is stored as byte, but Querz reads it
        val slot = NbtHelper.int(item, "Slot", -1)
        return ItemStack(id = id, count = count, slot = slot)
    }
}
