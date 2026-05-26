package org.hnau.upchain.sync.client.core

import arrow.core.raise.result
import co.touchlab.kermit.Logger
import org.hnau.commons.kotlin.foldNullable
import org.hnau.commons.kotlin.ifNull
import org.hnau.upchain.core.UpchainHash
import org.hnau.upchain.core.UpchainId
import org.hnau.upchain.core.Update
import org.hnau.upchain.core.plus
import org.hnau.upchain.core.repository.upchain.UpchainRepository
import org.hnau.upchain.sync.client.core.utils.RemoteUpdatesSink
import org.hnau.upchain.sync.client.core.utils.ServerUpdatesProvider
import org.hnau.upchain.sync.core.SyncApi

suspend fun UpchainRepository.sync(
    id: UpchainId,
    api: SyncApi,
): Result<Unit> = result {
    val logger = Logger.withTag("UpchainRepositorySyncExt.sync")

    var pushed = false
    while (!pushed) {

        logger.v { "Pulling updates from server" }
        val serverPeekHash = pull(
            id = id,
            api = api,
        ).bind()
        logger.v { "Pulled updates from server. Peek hash: $serverPeekHash. Pushing updates to server" }

        pushed = push(
            id = id,
            api = api,
            serverPeekHash = serverPeekHash,
        ).bind()
        logger.v { "Pushed updates to server. Success: $pushed" }
    }
    logger.d { "Sync finished" }
}

private suspend fun UpchainRepository.pull(
    id: UpchainId,
    api: SyncApi,
): Result<UpchainHash?> = result {
    val logger = Logger.withTag("UpchainRepositorySyncExt.pull")

    val serverUpdates = ServerUpdatesProvider(
        id = id,
        api = api,
    )

    editWithResult { upchain ->

        val serverUpdatesToApply = mutableListOf<Update>()
        var commonUpdatesCount: Int? = null

        loop@ while (commonUpdatesCount == null) {

            val remoteItem = serverUpdates
                .getNextUpdate()
                .bind()
                .ifNull {
                    logger.v { "Server has no more updates" }
                    break@loop
                }

            upchain
                .indexesByHash[remoteItem.hash]
                .foldNullable(
                    ifNull = { serverUpdatesToApply += remoteItem.update },
                    ifNotNull = { localIndexOfHash ->
                        logger.v { "Found last common update (hash: ${remoteItem.hash})" }
                        commonUpdatesCount = localIndexOfHash + 1
                    },
                )
        }

        serverUpdatesToApply
            .takeIf(Collection<*>::isNotEmpty)
            .foldNullable(
                ifNull = { logger.v { "There are no updates from server to apply" } },
                ifNotNull = { logger.v { "Applying ${it.size} updates from server" } }
            )

        val actualCommonCount = commonUpdatesCount ?: 0
        val (commonBase, afterServer) = upchain.take(actualCommonCount)
        val serverState = commonBase + serverUpdatesToApply.asReversed()
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