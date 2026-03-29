package org.hnau.upchain.sync.server.utils

import arrow.core.raise.result
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.hnau.upchain.core.UpchainHash
import org.hnau.upchain.core.Update
import org.hnau.upchain.core.plus
import org.hnau.upchain.core.repository.upchain.UpchainRepository
import org.hnau.upchain.sync.core.SyncHandle

internal class UpchainSyncServer(
    private val repository: UpchainRepository,
) {

    private val accessStateMutex: Mutex = Mutex()

    private suspend inline fun <R> withRepository(
        block: (UpchainRepository) -> R,
    ): R = accessStateMutex.withLock {
        block(repository)
    }


    data class Upchain(
        val peekHash: UpchainHash?,
    )

    suspend fun getUpchain(): Upchain = withRepository { upchainRepository ->
        Upchain(
            peekHash = upchainRepository.upchain.value.peekHash,
        )
    }

    suspend fun getMaxToMinUpdates(
        before: UpchainHash?,
    ): Result<SyncHandle.GetMaxToMinUpdates.Response> = withRepository { upchainRepository ->
        result {
            val upchain = upchainRepository
                .upchain
                .value
            val totalCount = upchain.items.size
            val beforeIndex = before?.let { beforeNotNull ->
                runCatching { upchain.indexesByHash.getValue(beforeNotNull) }.bind()
            }
            val dropLastCount = when (beforeIndex) {
                null -> 0
                else -> totalCount - beforeIndex
            }
            SyncHandle.GetMaxToMinUpdates.Response(
                updates = upchain
                    .items
                    .asReversed()
                    .asSequence()
                    .drop(dropLastCount)
                    .take(UpchainSyncServerConstants.updatesToSendPortionSize)
                    .toList(),
                hasMoreUpdates = totalCount > dropLastCount + UpchainSyncServerConstants.updatesToSendPortionSize
            )
        }
    }

    suspend fun appendUpdates(
        peekHashToCheck: UpchainHash?,
        updates: List<Update>,
    ): Result<Unit> = withRepository { upchainRepository ->
        result {
            upchainRepository.edit { currentUpchain ->
                if (currentUpchain.peekHash != peekHashToCheck) {
                    raise(IllegalStateException("Incorrect peek hash"))
                }
                currentUpchain + updates
            }
        }
    }
}