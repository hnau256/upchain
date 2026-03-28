package org.hnau.upchain.sync.client.utils

import org.hnau.upchain.core.Upchain
import org.hnau.upchain.core.UpchainHash
import org.hnau.upchain.core.Update

internal inline fun Upchain.merge(
    getNextMaxToMinOtherItem: () -> Upchain.Item?,
    addUpdateToOther: (update: Update, previousPeek: UpchainHash?) -> Unit,
): Upchain {
    var equalsUpdatesCountOrNull: Int? = null
    val updatesToAddMaxToMin: MutableList<Update> = mutableListOf()
    var remoteHasMoreUpdates = true
    do {
        val nextMaxToMinOtherItem = getNextMaxToMinOtherItem()
        when (nextMaxToMinOtherItem) {
            null -> remoteHasMoreUpdates = false
            else -> {
                val (update, hash) = nextMaxToMinOtherItem
                val localIndexOfHash = indexesByHash[hash]
                when (localIndexOfHash) {
                    null -> updatesToAddMaxToMin += update
                    else -> equalsUpdatesCountOrNull = localIndexOfHash + 1
                }
            }
        }
    } while (equalsUpdatesCountOrNull == null && remoteHasMoreUpdates)

    val equalsUpdatesCount = equalsUpdatesCountOrNull ?: 0
    val (maxLocalAndRemoteSameHead, updatesToPush) = take(equalsUpdatesCount)

    var result = updatesToAddMaxToMin
        .asReversed()
        .fold(
            initial = maxLocalAndRemoteSameHead,
        ) { acc, update ->
            acc + update
        }

    updatesToPush
        .forEach { update ->
            addUpdateToOther(
                update,
                result.peekHash,
            )
            result += update
        }

    return result
}