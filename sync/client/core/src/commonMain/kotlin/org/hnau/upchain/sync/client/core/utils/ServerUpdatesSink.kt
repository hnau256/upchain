package org.hnau.upchain.sync.client.core.utils

import arrow.core.raise.result
import org.hnau.commons.gen.loggable.annotations.Loggable
import org.hnau.commons.kotlin.ifTrue
import org.hnau.upchain.core.Upchain
import org.hnau.upchain.core.UpchainHash
import org.hnau.upchain.core.UpchainId
import org.hnau.upchain.core.Update
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.core.SyncHandle

@Loggable
internal class RemoteUpdatesSink(
    private val id: UpchainId,
    private val api: SyncApi,
    initialServerPeekHash: UpchainHash?,
) {
    private var serverPeekBeforeBuffer: UpchainHash? = initialServerPeekHash

    private val buffer = mutableListOf<Update>()

    suspend fun push(
        item: Upchain.Item,
    ): Result<Boolean> = result {
        buffer += item.update
        if (buffer.size < UpchainSyncClientConstants.updatesToSendPortionSize) {
            return@result true
        }
        val result = flush().bind()
        result.ifTrue { serverPeekBeforeBuffer = item.hash }
        result
    }

    suspend fun flush(): Result<Boolean> = result {
        if (buffer.isEmpty()) {
            logger.v { "No need to push: no updates" }
            return@result true
        }

        val pushed = api
            .handle(
                SyncHandle.AppendUpdates(
                    upchainId = id,
                    peekHashToCheck = serverPeekBeforeBuffer,
                    updates = buffer,
                ),
            )
            .bind()
            .let { response ->
                when (response) {
                    SyncHandle.AppendUpdates.Response.Success -> {
                        logger.v { "Updates was pushed" }
                        true
                    }
                    SyncHandle.AppendUpdates.Response.ServerAhead -> {
                        logger.w { "Updates was not pushed: server ahead" }
                        false
                    }
                }
            }

        pushed.ifTrue { buffer.clear() }

        pushed
    }
}
