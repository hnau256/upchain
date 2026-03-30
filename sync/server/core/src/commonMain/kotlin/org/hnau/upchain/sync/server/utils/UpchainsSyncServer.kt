package org.hnau.upchain.sync.server.utils

import arrow.core.identity
import arrow.core.raise.result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import org.hnau.commons.kotlin.coroutines.flow.state.mapListReusable
import org.hnau.commons.kotlin.coroutines.flow.state.mapState
import org.hnau.commons.kotlin.foldNullable
import org.hnau.upchain.core.UpchainHash
import org.hnau.upchain.core.UpchainId
import org.hnau.upchain.core.Update
import org.hnau.upchain.sync.core.SyncHandle
import org.hnau.upchain.sync.server.repository.UpchainsCreateOnlyRepository

class UpchainsSyncServer(
    scope: CoroutineScope,
    private val repository: UpchainsCreateOnlyRepository,
) {

    private val upchains: StateFlow<Map<UpchainId, UpchainSyncServer>> = repository
        .upchains
        .mapListReusable(
            scope = scope,
            extractKey = { it.key },
            transform = { _, (id, repository) ->
                val upchainSyncServer = UpchainSyncServer(
                    repository = repository,
                )
                id to upchainSyncServer
            }
        )
        .mapState(scope) { upchains ->
            upchains.associate(::identity)
        }


    data class Upchain(
        val id: UpchainId,
        val peekHash: UpchainHash?,
    )

    suspend fun getUpchains(): Result<List<Upchain>> = result {
        coroutineScope {
            upchains.value.let { servers ->
                servers
                    .mapValues { (_, server) ->
                        async { server.getUpchain() }
                    }
                    .map { (id, deferredUpchain) ->
                        val upchain = deferredUpchain.await()
                        Upchain(
                            id = id,
                            peekHash = upchain.peekHash,
                        )
                    }
            }
        }
    }

    suspend fun getMaxToMinUpdates(
        upchainId: UpchainId,
        before: UpchainHash?,
    ): Result<SyncHandle.GetMaxToMinUpdates.Response> = upchains
        .value[upchainId]
        .foldNullable(
            ifNull = {
                Result.success(
                    SyncHandle.GetMaxToMinUpdates.Response(
                        updates = emptyList(),
                        hasMoreUpdates = false,
                    )
                )
            },
            ifNotNull = { upchainSyncServer ->
                upchainSyncServer.getMaxToMinUpdates(
                    before = before,
                )
            }
        )

    suspend fun appendUpdates(
        upchainId: UpchainId,
        peekHashToCheck: UpchainHash?,
        updates: List<Update>,
    ): Result<SyncHandle.AppendUpdates.Response> = run {
        repository.createUpchain(upchainId)
        upchains.mapNotNull { it[upchainId] }.first()
    }.appendUpdates(
        peekHashToCheck = peekHashToCheck,
        updates = updates,
    )
}