package org.hnau.upchain.sync.core

fun SyncApi.observe(
    onMaxToMinUpdates: (maxToMinUpdatesCount: Int) -> Unit,
    onUpdatesToAppend: (updatesToAppendCount: Int) -> Unit,
): SyncApi = object : SyncApi {

    private val source: SyncApi
        get() = this@observe

    @Suppress("UNCHECKED_CAST")
    override suspend fun <O, I : SyncHandle<O>> handle(
        request: I
    ): Result<O> = when (request) {
        is SyncHandle.AppendUpdates -> appendUpdates(request)
        is SyncHandle.GetMaxToMinUpdates -> getMaxToMinUpdates(request)
    } as Result<O>

    private suspend fun getMaxToMinUpdates(
        request: SyncHandle.GetMaxToMinUpdates,
    ): Result<SyncHandle.GetMaxToMinUpdates.Response> = source
        .handle(
            request = request,
        )
        .onSuccess { response ->
            onMaxToMinUpdates(response.updates.size)
        }

    private suspend fun appendUpdates(
        request: SyncHandle.AppendUpdates,
    ): Result<SyncHandle.AppendUpdates.Response> = source
        .handle(
            request = request,
        )
        .onSuccess {
            onUpdatesToAppend(request.updates.size)
        }
}