package org.hnau.upchain.core.repository.upchain

import kotlinx.coroutines.flow.StateFlow
import org.hnau.upchain.core.Upchain

interface UpchainRepository {

    val upchain: StateFlow<Upchain>

    suspend fun <R> editWithResult(
        modify: suspend (Upchain) -> Pair<Upchain, R>,
    ): R

    companion object
}