package org.hnau.upchain.sync.client

import arrow.core.raise.result
import org.hnau.commons.kotlin.foldNullable
import org.hnau.commons.kotlin.ifNull
import org.hnau.upchain.core.UpchainHash
import org.hnau.upchain.core.UpchainId
import org.hnau.upchain.core.Update
import org.hnau.upchain.core.plus
import org.hnau.upchain.core.repository.upchain.UpchainRepository
import org.hnau.upchain.sync.client.utils.RemoteUpdatesSink
import org.hnau.upchain.sync.client.utils.ServerUpdatesProvider
import org.hnau.upchain.sync.client.utils.TcpSyncClient
import org.hnau.upchain.sync.core.ServerPort
import org.hnau.upchain.sync.core.SyncApi

suspend fun UpchainRepository.sync(
    id: UpchainId,
    remoteAddress: ServerAddress,
    remotePort: ServerPort,
): Result<Unit> = result {

    val api: SyncApi = TcpSyncClient(
        address = remoteAddress,
        port = remotePort,
    )

    var pushed = false
    while (!pushed) {

        val serverPeekHash = pull(
            id = id,
            api = api,
        ).bind()

        pushed = push(
            id = id,
            api = api,
            serverPeekHash = serverPeekHash,
        ).bind()
    }
}

private suspend fun UpchainRepository.pull(
    id: UpchainId,
    api: SyncApi,
): Result<UpchainHash?> = result {

    val serverUpdates = ServerUpdatesProvider(
        id = id,
        api = api,
    )

    editWithResult { upchain ->

        val serverUpdatesToApply = mutableListOf<Update>()
        var commonUpdatesCount: Int? = null

        while (commonUpdatesCount == null) {

            val remoteItem = serverUpdates
                .getNextUpdate()
                .bind()
                ?: break

            upchain
                .indexesByHash[remoteItem.hash]
                .foldNullable(
                    ifNull = { serverUpdatesToApply += remoteItem.update },
                    ifNotNull = { localIndexOfHash ->
                        commonUpdatesCount = localIndexOfHash + 1
                    },
                )
        }

        val actualCommonCount = commonUpdatesCount ?: 0
        val (commonBase, afterServer) = upchain.take(actualCommonCount)
        val serverState = commonBase + serverUpdatesToApply
        val merged = serverState + afterServer

        merged to serverState.peekHash
    }

}

private suspend fun UpchainRepository.push(
    id: UpchainId,
    api: SyncApi,
    serverPeekHash: UpchainHash?,
): Result<Boolean> = result {

    val sink = RemoteUpdatesSink(
        id = id,
        api = api,
        initialServerPeekHash = serverPeekHash,
    )

    val upchain = upchain.value

    val commonBaseSize = serverPeekHash
        ?.let {
            upchain
                .indexesByHash[it]
                .ifNull { return@result false }
                .plus(1)
        }
        ?: 0

    val pushed = upchain
        .items
        .drop(commonBaseSize)
        .fold(
            initial = true,
        ) { acc, item ->
            acc && sink.push(item).bind()
        }

    pushed && sink.flush().bind()
}