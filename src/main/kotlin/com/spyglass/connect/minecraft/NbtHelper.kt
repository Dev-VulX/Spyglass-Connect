package com.spyglass.connect.minecraft

import net.querz.nbt.io.NBTUtil
import net.querz.nbt.io.NamedTag
import net.querz.nbt.tag.*
import java.io.File

/**
 * Utility wrappers around Querz NBT for safe, null-tolerant compound navigation.
 * Mirrors the `safe()` pattern from minecolony-manager/parser/colony_parser.py.
 */
object NbtHelper {

    /** Read a compressed NBT file (gzipped, like level.dat and playerdata). */
    fun readCompressed(path: File): CompoundTag? {
        if (!path.exists()) return null
        return try {
            val named: NamedTag = NBTUtil.read(path)
            named.tag as? CompoundTag
        } catch (e: Exception) {
            null
        }
    }

    /** Safe multi-key navigation into nested compounds. Returns null on any miss. */
    fun safe(root: CompoundTag?, vararg keys: String): Tag<*>? {
        var current: Tag<*>? = root
        for (key in keys) {
            current = when (current) {
                is CompoundTag -> current.get(key)
                else -> return null
            }
        }
        return current
    }

    /** Get a nested compound tag safely. */
    fun compound(root: CompoundTag?, vararg keys: String): CompoundTag? =
        safe(root, *keys) as? CompoundTag

    /** Get a nested list tag safely. */
    @Suppress("UNCHECKED_CAST")
    fun <T : Tag<*>> list(root: CompoundTag?, vararg keys: String): ListTag<T>? =
        safe(root, *keys) as? ListTag<T>

    /** Get an int value with a default. */
    fun int(root: CompoundTag?, key: String, default: Int = 0): Int =
        when (val tag = root?.get(key)) {
            is IntTag -> tag.asInt()
            is ShortTag -> tag.asShort().toInt()
            is ByteTag -> tag.asByte().toInt()
            is LongTag -> tag.asLong().toInt()
            else -> default
        }

    /** Get a long value with a default. */
    fun long(root: CompoundTag?, key: String, default: Long = 0L): Long =
        when (val tag = root?.get(key)) {
            is LongTag -> tag.asLong()
            is IntTag -> tag.asInt().toLong()
            else -> default
        }

    /** Get a float value with a default. */
    fun float(root: CompoundTag?, key: String, default: Float = 0f): Float =
        when (val tag = root?.get(key)) {
            is FloatTag -> tag.asFloat()
            is DoubleTag -> tag.asDouble().toFloat()
            is IntTag -> tag.asInt().toFloat()
            else -> default
        }

    /** Get a double value with a default. */
    fun double(root: CompoundTag?, key: String, default: Double = 0.0): Double =
        when (val tag = root?.get(key)) {
            is DoubleTag -> tag.asDouble()
            is FloatTag -> tag.asFloat().toDouble()
            else -> default
        }

    /** Get a string value with a default. */
    fun string(root: CompoundTag?, key: String, default: String = ""): String =
        when (val tag = root?.get(key)) {
            is StringTag -> tag.value ?: default
            else -> tag?.valueToString() ?: default
        }

    /** Get a byte (boolean) value with a default. */
    fun boolean(root: CompoundTag?, key: String, default: Boolean = false): Boolean =
        when (val tag = root?.get(key)) {
            is ByteTag -> tag.asByte().toInt() != 0
            else -> default
        }
}
