package org.hnau.upchain.sync.server.core

import kotlinx.coroutines.CoroutineScope
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.core.SyncHandle
import org.hnau.upchain.sync.server.core.repository.UpchainsCreateOnlyRepository
import org.hnau.upchain.sync.server.core.utils.UpchainsSyncServer

class ServerEngine(
    scope: CoroutineScope,
    repository: UpchainsCreateOnlyRepository,
) : SyncApi {

    private val syncServer = UpchainsSyncServer(
        scope = scope,
        repository = repository,
    )

    @Suppress("UNCHECKED_CAST")
    override suspend fun <O, I : SyncHandle<O>> handle(
        request: I,
    ): Result<O> = when (request) {

        is SyncHandle.GetMaxToMinUpdates ->
            getMaxToMinUpdates(request) as Result<O>

        is SyncHandle.AppendUpdates ->
            appendUpdates(request) as Result<O>
    }

    private suspend fun getMaxToMinUpdates(
        request: SyncHandle.GetMaxToMinUpdates,
    ): Result<SyncHandle.GetMaxToMinUpdates.Response> = syncServer.getMaxToMinUpdates(
        upchainId = request.upchainId,
        before = request.before,
    )

    private suspend fun appendUpdates(
        request: SyncHandle.AppendUpdates,
    ): Result<SyncHandle.AppendUpdates.Response> = syncServer.appendUpdates(
        upchainId = request.upchainId,
        peekHashToCheck = request.peekHashToCheck,
        updates = request.updates,
    )
}