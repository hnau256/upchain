package org.hnau.upchain.sync.server.core.repository

import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.kotlin.KeyValue
import org.hnau.upchain.core.UpchainId
import org.hnau.upchain.core.repository.upchain.UpchainRepository

interface UpchainsCreateOnlyRepository {

    val upchains: StateFlow<List<KeyValue<UpchainId, UpchainRepository>>>

    suspend fun createUpchain(
        id: UpchainId,
    )

    companion object
}