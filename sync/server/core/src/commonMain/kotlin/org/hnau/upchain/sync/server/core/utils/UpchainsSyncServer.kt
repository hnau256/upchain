package org.hnau.upchain.sync.server.core.utils

import arrow.core.identity
import kotlinx.coroutines.CoroutineScope
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
import org.hnau.upchain.sync.server.core.repository.UpchainsCreateOnlyRepository

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