package org.hnau.upchain.sync.client.tcp

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
import kotlinx.serialization.ExperimentalSerializationApi
import org.hnau.upchain.sync.client.core.ClientSerializedEngine
import org.hnau.upchain.sync.core.ServerAddress
import org.hnau.upchain.sync.core.ServerPort
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.core.SyncHandle
import org.hnau.upchain.sync.core.utils.SyncConstants
import org.hnau.upchain.sync.tcp.CborTransportMapper
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

    private val selectorManager: SelectorManager = SelectorManager(Dispatchers.IO)

    private val serialized: ClientSerializedEngine<ByteArray> = run {

        val socketBuilder = aSocket(selectorManager).tcp()

        ClientSerializedEngine(
            serverAddress = address.address,
            transportMapper = CborTransportMapper,
        ) { request ->

            socketBuilder
                .connect(
                    remoteAddress = InetSocketAddress(
                        hostname = address.address,
                        port = port.port,
                    ),
                )
                .use { socket ->
                    socket
                        .openWriteChannel()
                        .writeSizeWithBytes(
                            bytes = request,
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
    ): Result<O> = serialized.handle(
        request = request,
    )
}