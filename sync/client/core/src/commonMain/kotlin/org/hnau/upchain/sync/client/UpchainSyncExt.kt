package org.hnau.upchain.sync.client

import arrow.core.raise.result
import org.hnau.upchain.core.Upchain
import org.hnau.upchain.core.UpchainHash
import org.hnau.upchain.core.Update
import org.hnau.upchain.sync.client.utils.UpchainSyncClientConstants
import org.hnau.upchain.sync.client.utils.merge
import org.hnau.upchain.sync.core.SyncHandle
import org.hnau.upchain.sync.core.UpchainId

suspend fun Upchain.syncWithRemote(
    upchainId: UpchainId,
    remote: TcpSyncClient,
): Result<Upchain> = result {

    var remoteHasMoreUpdates = true
    var minReceivedRemoteHash: UpchainHash? = null
    val remoteUpdatesBuffer: MutableList<Upchain.Item> = mutableListOf()

    var remotePeek: UpchainHash? = null
    val updatesToPush: MutableList<Update> = mutableListOf()

    suspend fun flushUpdates(): Result<Unit> {
        if (updatesToPush.isEmpty()) {
            return Result.success(Unit)
        }
        return remote
            .handle(
                SyncHandle.AppendUpdates(
                    upchainId = upchainId,
                    peekHashToCheck = remotePeek,
                    updates = updatesToPush,
                )
            )
            .map { }
            .onSuccess {
                updatesToPush.clear()
            }
    }

    val result = merge(
        getNextMaxToMinOtherItem = {
            if (remoteUpdatesBuffer.isEmpty()) {
                if (remoteHasMoreUpdates) {
                    val getUpdatesResult = remote
                        .handle(
                            SyncHandle.GetMaxToMinUpdates(
                                upchainId = upchainId,
                                before = minReceivedRemoteHash,
                            )
                        )
                        .bind()
                    val updates = getUpdatesResult.updates
                    minReceivedRemoteHash = updates.lastOrNull()?.hash
                    remoteUpdatesBuffer.addAll(updates)
                    remoteHasMoreUpdates = getUpdatesResult.hasMoreUpdates
                }
            }
            remoteUpdatesBuffer.removeFirstOrNull()
        },
        addUpdateToOther = { update, previousPeek ->
            if (updatesToPush.isEmpty()) {
                remotePeek = previousPeek
            }
            updatesToPush += update
            if (updatesToPush.size >= UpchainSyncClientConstants.updatesToSendPortionSize) {
                flushUpdates().bind()
            }
        },
    )

    flushUpdates().bind()

    result
}