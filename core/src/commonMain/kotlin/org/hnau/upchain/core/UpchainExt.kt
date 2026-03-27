package org.hnau.upchain.core

operator fun Upchain.plus(
    updates: Iterable<Update>,
): Upchain = updates.fold(
    initial = this,
    operation = Upchain::plus,
)

fun Upchain.getUpdatesAfterHashIfPossible(
    hash: UpchainHash?,
): List<Update>? = when (hash) {
    null -> take(0)
    else -> indexesByHash[hash]?.let { indexOfHash -> take(indexOfHash + 1) }
}?.second