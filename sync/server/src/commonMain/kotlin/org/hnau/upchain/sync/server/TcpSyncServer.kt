package org.hnau.upchain.sync.server

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import org.hnau.upchain.sync.core.ApiResponse
import org.hnau.upchain.sync.core.ServerPort
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.core.SyncHandle
import org.hnau.upchain.sync.core.utils.SyncConstants
import org.hnau.upchain.sync.core.utils.readSizeWithBytes
import org.hnau.upchain.sync.core.utils.writeSizeWithBytes
import org.hnau.upchain.sync.server.repository.UpchainsCreateOnlyRepository
import org.hnau.upchain.sync.server.utils.ServerSyncApi
import kotlin.time.Duration

suspend fun tcpSyncServer(
    port: ServerPort,
    repository: UpchainsCreateOnlyRepository,
    tcpTimeout: Duration = SyncConstants.tcpTimeout,
    onThrowable: (Throwable) -> Unit,
): Result<Nothing> = coroutineScope {
    runCatching {
        val api = ServerSyncApi(
            scope = this@coroutineScope,
            repository = repository,
        )
        val selectorManager = SelectorManager(Dispatchers.IO)
        withContext(Dispatchers.IO) {
            val serverSocket =
                aSocket(selectorManager)
                    .tcp()
                    .bind(port = port.port)
            try {
                while (true) {
                    try {
                        circle(
                            serverSocket = serverSocket,
                            api = api,
                            onThrowable = onThrowable,
                            timeout = tcpTimeout,
                        )
                    } catch (ex: CancellationException) {
                        throw ex
                    } catch (th: Throwable) {
                        onThrowable(th)
                    }
                }
                awaitCancellation()
            } finally {
                try {
                    serverSocket.close()
                    selectorManager.close()
                } catch (ex: CancellationException) {
                    throw ex
                } catch (th: Throwable) {
                    onThrowable(th)
                }
            }
        }
    }
}

private suspend fun CoroutineScope.circle(
    serverSocket: ServerSocket,
    api: SyncApi,
    timeout: Duration,
    onThrowable: (Throwable) -> Unit,
) {
    val clientSocket = serverSocket.accept()
    launch {
        try {
            clientSocket.use { clientSocket ->
                val requestBytes = clientSocket
                    .openReadChannel()
                    .readSizeWithBytes(
                        timeout = timeout,
                    )
                val responseBytes = api.handle(requestBytes)
                clientSocket
                    .openWriteChannel()
                    .writeSizeWithBytes(
                        bytes = responseBytes,
                        timeout = timeout,
                    )
            }
        } catch (ex: CancellationException) {
            throw ex
        } catch (th: Throwable) {
            onThrowable(th)
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
private suspend fun SyncApi.handle(
    request: ByteArray,
): ByteArray {
    val typedRequest = SyncConstants.cbor.decodeFromByteArray(
        SyncHandle.serializer,
        request,
    )
    return handleTyped(typedRequest)
}

private suspend fun <O, I : SyncHandle<O>> SyncApi.handleTyped(
    request: I,
): ByteArray = handle(request)
    .fold(
        onSuccess = { result -> ApiResponse.Success(result) },
        onFailure = { error -> ApiResponse.Error(error.message) },
    )
    .let { response ->
        ApiResponse
            .createByteArrayMapper(request.responseSerializer)
            .reverse(response)
    }
