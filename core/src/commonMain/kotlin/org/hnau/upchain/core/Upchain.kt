package org.hnau.upchain.core

import kotlinx.serialization.Serializable
import org.hnau.commons.kotlin.castOrNull
import org.hnau.upchain.core.utils.ContentHash

class Upchain private constructor(
    val items: List<Item>,
    val indexesByHash: Map<UpchainHash, Int>,
    val contentHashes: Set<ContentHash>,
) {

    @Serializable
    data class Item(
        val update: Update,
        val hash: UpchainHash,
    )

    val peekHash: UpchainHash? =
        items.lastOrNull()?.hash


    operator fun plus(
        update: Update,
    ): Upchain {
        val newHash = peekHash.calcNext(
            update = update,
        )
        val item = Item(
            update = update,
            hash = newHash,
        )
        val itemHash = item.contentHash
        if (itemHash in contentHashes) {
            //return this TODO
        }
        return Upchain(
            items = items + item,
            indexesByHash = indexesByHash + (newHash to items.size),
            contentHashes = contentHashes + itemHash,
        )
    }

    fun take(
        count: Int,
    ): Pair<Upchain, List<Update>> {
        val newItems = items.take(count)
        val upchain = Upchain(
            items = newItems,
            indexesByHash = indexesByHash.filterValues { it < count },
            contentHashes = newItems
                .map { item -> item.contentHash }
                .toSet(),
        )
        val detachedUpdates = items
            .drop(count)
            .map(Item::update)
        return upchain to detachedUpdates
    }

    private val Item.contentHash: ContentHash
        get() = ContentHash.create(update)

    override fun equals(
        other: Any?,
    ): Boolean = other
        ?.castOrNull<Upchain>()
        ?.takeIf { it.peekHash == peekHash } != null

    override fun hashCode(): Int = peekHash
        ?.hashCode()
        ?: 0

    companion object {

        val empty: Upchain = Upchain(
            items = emptyList(),
            indexesByHash = emptyMap(),
            contentHashes = emptySet(),
        )
    }
}