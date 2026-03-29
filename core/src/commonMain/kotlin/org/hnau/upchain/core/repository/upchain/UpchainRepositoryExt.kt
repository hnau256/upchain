package org.hnau.upchain.core.repository.upchain

import org.hnau.upchain.core.Update
import org.hnau.upchain.core.plus

suspend fun UpchainRepository.addUpdates(
    updates: Iterable<Update>,
) {
    edit { upchain -> upchain + updates }
}

suspend fun UpchainRepository.addUpdate(
    update: Update,
) {
    addUpdates(
        updates = listOf(update)
    )
}