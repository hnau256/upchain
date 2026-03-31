package org.hnau.upchain.sync.client.core.utils

import arrow.core.flatMap
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import org.hnau.upchain.sync.client.core.ServerAddress
import org.hnau.upchain.sync.core.ApiResponse
import org.hnau.upchain.sync.core.ServerPort
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.core.SyncHandle
import org.hnau.upchain.sync.core.utils.SyncConstants
import org.hnau.upchain.sync.core.utils.readSizeWithBytes
import org.hnau.upchain.sync.core.utils.writeSizeWithBytes
import kotlin.time.Duration

internal class TcpSyncClient(
    private val address: ServerAddress,
    private val port: ServerPort,
    private val tcpTimeout: Duration = SyncConstants.tcpTimeout,
) : SyncApi {

    private val selectorManager = SelectorManager(Dispatchers.IO)

    private val socketBuilder = aSocket(selectorManager).tcp()

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <O, I : SyncHandle<O>> handle(
        request: I,
    ): Result<O> = runCatching {
        val requestBytes = withContext(Dispatchers.Default) {
            SyncConstants.cbor.encodeToByteArray(SyncHandle.serializer, request)
        }
        val responseBytes = withContext(Dispatchers.IO) {
            withTimeout(tcpTimeout) {
                socketBuilder
                    .connect(
                        remoteAddress =
                            InetSocketAddress(
                                hostname = address.address,
                                port = port.port,
                            ),
                        configure = {
                            // Ktor network doesn't have direct socketTimeout,
                            // timeouts are handled by withTimeout
                        },
                    )
                    .use { socket ->
                        socket
                            .openWriteChannel()
                            .writeSizeWithBytes(
                                bytes = requestBytes,
                                timeout = tcpTimeout,
                            )
                        socket
                            .openReadChannel()
                            .readSizeWithBytes(
                                timeout = tcpTimeout,
                            )
                    }
            }
        }
        val response = withContext(Dispatchers.Default) {
            ApiResponse
                .createByteArrayMapper(
                    dataSerializer = request.responseSerializer,
                )
                .direct(responseBytes)
        }
        response
    }.flatMap { response ->
        when (response) {
            is ApiResponse.Success ->
                Result.success(response.data)

            is ApiResponse.Error ->
                Result.failure(Exception("Error received from sync server: ${response.error}"))
        }
    }

    fun close() {
        selectorManager.close()
    }
}
