package org.hnau.upchain.sync.client.tcp

import arrow.core.flatMap
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import org.hnau.upchain.sync.core.ApiResponse
import org.hnau.upchain.sync.core.ServerAddress
import org.hnau.upchain.sync.core.ServerPort
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.core.SyncHandle
import org.hnau.upchain.sync.core.utils.SyncConstants
import org.hnau.upchain.sync.tcp.SyncConstantsTcp
import org.hnau.upchain.sync.tcp.createCborMapper
import org.hnau.upchain.sync.tcp.defaultTcp
import org.hnau.upchain.sync.tcp.readSizeWithBytes
import org.hnau.upchain.sync.tcp.writeSizeWithBytes
import kotlin.time.Duration

internal class TcpSyncClient(
    scope: CoroutineScope,
    private val address: ServerAddress,
    private val tcpTimeout: Duration = SyncConstants.tcpTimeout,
    private val port: ServerPort = ServerPort.defaultTcp,
) : SyncApi {

    private val selectorManager = SelectorManager(Dispatchers.IO)

    private val socketBuilder = aSocket(selectorManager).tcp()

    init {
        scope.launch {
            try {
                awaitCancellation()
            } catch (ex: CancellationException) {
                selectorManager.close()
                throw ex
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <O, I : SyncHandle<O>> handle(
        request: I,
    ): Result<O> = runCatching {
        val requestBytes = withContext(Dispatchers.Default) {
            SyncConstantsTcp.cbor.encodeToByteArray(SyncHandle.serializer, request)
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
                .createCborMapper(
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
}