package com.spyglass.connect.minecraft

import com.spyglass.connect.model.ContainerInfo
import com.spyglass.connect.model.SearchHit

/**
 * In-memory inverted index for fast item search across all containers.
 * Maps item IDs → container locations with counts.
 *
 * Port of minecolony-manager/parser/storage_parser.py merge_inventory() aggregation.
 */
class ItemSearchIndex {

    /** item ID → list of containers that hold it (with per-container count) */
    private val index = mutableMapOf<String, MutableList<ContainerHit>>()

    /** All item IDs in the index, for prefix matching. */
    private val allItemIds = mutableSetOf<String>()

    data class ContainerHit(
        val container: ContainerInfo,
        val count: Int,
    )

    /** Build the index from a list of scanned containers. */
    fun build(containers: List<ContainerInfo>) {
        index.clear()
        allItemIds.clear()

        for (container in containers) {
            // Aggregate items by ID within this container
            val byItem = mutableMapOf<String, Int>()
            for (item in container.items) {
                byItem[item.id] = (byItem[item.id] ?: 0) + item.count
            }

            for ((itemId, count) in byItem) {
                allItemIds.add(itemId)
                index.getOrPut(itemId) { mutableListOf() }
                    .add(ContainerHit(container, count))
            }
        }
    }

    /** Search for items matching a query string. Supports prefix and substring matching. */
    fun search(query: String, maxResults: Int = 50): List<SearchHit> {
        val q = query.lowercase().trim()
        if (q.isBlank()) return emptyList()

        // Find matching item IDs: exact > prefix > substring
        val matches = allItemIds.filter { id ->
            id == q || id.contains(q)
        }.sortedWith(compareBy(
            { if (it == q) 0 else if (it.startsWith(q)) 1 else 2 },
            { it },
        ))

        return matches.take(maxResults).map { itemId ->
            val hits = index[itemId] ?: emptyList()
            SearchHit(
                itemId = itemId,
                totalCount = hits.sumOf { it.count },
                locations = hits.map { hit ->
                    hit.container.copy(
                        items = hit.container.items.filter { it.id == itemId },
                    )
                },
            )
        }
    }

    /** Get all items with their total counts, sorted by count descending. */
    fun allItems(): List<Pair<String, Int>> {
        return index.map { (itemId, hits) ->
            itemId to hits.sumOf { it.count }
        }.sortedByDescending { it.second }
    }

    /** Get total unique item types. */
    val uniqueItemCount: Int get() = allItemIds.size

    /** Get total containers indexed. */
    val containerCount: Int
        get() = index.values.flatten().map {
            Triple(it.container.x, it.container.y, it.container.z)
        }.toSet().size

    fun clear() {
        index.clear()
        allItemIds.clear()
    }
}
