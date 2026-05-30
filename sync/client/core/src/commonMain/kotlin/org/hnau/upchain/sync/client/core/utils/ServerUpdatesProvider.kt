package org.hnau.upchain.sync.client.core.utils

import arrow.core.NonEmptyList
import arrow.core.raise.result
import arrow.core.toNonEmptyListOrNull
import org.hnau.commons.kotlin.foldNullable
import org.hnau.upchain.core.Upchain
import org.hnau.upchain.core.UpchainHash
import org.hnau.upchain.core.UpchainId
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.core.SyncHandle

internal class ServerUpdatesProvider(
    id: UpchainId,
    api: SyncApi,
    onUpdatesFromServers: (updatesCount: Int) -> Unit,
) {

    private class BatchesProvider(
        private val id: UpchainId,
        private val server: SyncApi,
        private val onUpdatesFromServers: (updatesCount: Int) -> Unit,
    ) {

        private sealed interface ServerState {

            data class InProgress(
                val lastMinHash: UpchainHash?,
            ) : ServerState

            data object NoMoreUpdates : ServerState
        }

        private var serverState: ServerState = ServerState.InProgress(
            lastMinHash = null,
        )

        suspend fun getNextBatch(): Result<NonEmptyList<Upchain.Item>?> = result {
            when (val localServerState = serverState) {
                ServerState.NoMoreUpdates -> null
                is ServerState.InProgress -> {

                    val response = server
                        .handle(
                            SyncHandle.GetMaxToMinUpdates(
                                upchainId = id,
                                before = localServerState.lastMinHash,
                            )
                        )
                        .bind()

                    val updates = response.updates
                    onUpdatesFromServers(updates.size)

                    val nonEmptyUpdates = updates.toNonEmptyListOrNull()

                    serverState = nonEmptyUpdates
                        ?.takeIf { response.hasMoreUpdates }
                        ?.last()
                        ?.hash
                        .foldNullable(
                            ifNull = { ServerState.NoMoreUpdates },
                            ifNotNull = ServerState::InProgress,
                        )

                    nonEmptyUpdates
                }
            }
        }
    }

    private val batchesProvider = BatchesProvider(
        id = id,
        server = api,
        onUpdatesFromServers = onUpdatesFromServers,
    )

    private var currentBatch: NonEmptyList<Upchain.Item>? = null

    suspend fun getNextUpdate(): Result<Upchain.Item?> = result {
        var batch = currentBatch
        if (batch == null) {
            batch = batchesProvider.getNextBatch().bind()
            currentBatch = batch
        }
        if (batch == null) {
            return@result null
        }
        currentBatch = batch.tail.toNonEmptyListOrNull()
        batch.head
    }
}