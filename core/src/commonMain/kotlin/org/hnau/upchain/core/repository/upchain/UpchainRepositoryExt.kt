package org.hnau.upchain.core.repository.upchain

import org.hnau.upchain.core.Upchain
import org.hnau.upchain.core.Update
import org.hnau.upchain.core.plus

suspend fun UpchainRepository.update(
    block: (Upchain) -> Upchain,
) {
    while (true) {
        val current = upchain.value
        val new = block(current)
        val updated = setNewUpchain(
            currentUpchainToCheck = current,
            newUpchain = new,
        )
        if (updated) {
            return
        }
    }
}

suspend fun UpchainRepository.addUpdates(
    updates: Iterable<Update>,
) {
    update { upchain -> upchain + updates }
}

suspend fun UpchainRepository.addUpdate(
    update: Update,
) {
    addUpdates(
        updates = listOf(update)
    )
}