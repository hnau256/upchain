package org.hnau.upchain.sync.core

interface SyncApi {

    suspend fun <O, I: SyncHandle<O>> handle(
        request: I,
    ): Result<O>
}