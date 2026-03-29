package org.hnau.upchain.core.repository.upchains

import kotlinx.coroutines.flow.StateFlow
import org.hnau.upchain.core.UpchainId
import org.hnau.upchain.core.repository.upchain.UpchainRepository

interface UpchainsRepository {

    data class Item(
        val id: UpchainId,
        val repository: UpchainRepository,
        val remove: suspend () -> Unit,
    )

    val upchains: StateFlow<List<Item>>

    suspend fun createUpchain(
        id: UpchainId,
    )

    companion object
}