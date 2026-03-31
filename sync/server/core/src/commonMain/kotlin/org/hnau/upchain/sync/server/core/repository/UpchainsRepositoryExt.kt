package org.hnau.upchain.sync.server.core.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.kotlin.KeyValue
import org.hnau.commons.kotlin.coroutines.flow.state.mapState
import org.hnau.upchain.core.UpchainId
import org.hnau.upchain.core.repository.upchain.UpchainRepository
import org.hnau.upchain.core.repository.upchains.UpchainsRepository

fun UpchainsRepository.toCreateOnly(
    scope: CoroutineScope,
): UpchainsCreateOnlyRepository = object : UpchainsCreateOnlyRepository {

    private val source: UpchainsRepository
        get() = this@toCreateOnly

    override val upchains: StateFlow<List<KeyValue<UpchainId, UpchainRepository>>> = source
        .upchains
        .mapState(scope) { items ->
            items.map { item ->
                KeyValue(
                    key = item.id,
                    value = item.repository,
                )
            }
        }

    override suspend fun createUpchain(
        id: UpchainId,
    ) {
        source.createUpchain(
            id = id,
        )
    }
}