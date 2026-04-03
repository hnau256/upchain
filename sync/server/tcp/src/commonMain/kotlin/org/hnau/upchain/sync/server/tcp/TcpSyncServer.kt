package org.hnau.upchain.sync.server.tcp

import co.touchlab.kermit.Logger
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hnau.upchain.sync.core.ServerPort
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.core.utils.SyncConstants
import org.hnau.upchain.sync.server.core.ServerSerializedEngine
import org.hnau.upchain.sync.tcp.CborTransportMapperFactory
import org.hnau.upchain.sync.tcp.defaultTcp
import org.hnau.upchain.sync.tcp.readSizeWithBytes
import org.hnau.upchain.sync.tcp.writeSizeWithBytes
import kotlin.time.Duration

private val logger: Logger = Logger.withTag("TcpSyncServer")

suspend fun tcpSyncServer(
    engine: SyncApi,
    tcpTimeout: Duration = SyncConstants.tcpTimeout,
    port: ServerPort = ServerPort.defaultTcp,
): Result<Nothing> = runCatching {

    val selectorManager = SelectorManager(Dispatchers.IO)

    val serializedEngine: ServerSerializedEngine<ByteArray> = ServerSerializedEngine(
        engine = engine,
        transportMapperFactory = CborTransportMapperFactory,
    )

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
                        engine = serializedEngine,
                        timeout = tcpTimeout,
                    )
                } catch (ex: CancellationException) {
                    throw ex
                } catch (th: Throwable) {
                    logError(th) { "main circle" }
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
                logError(th) { "stopping server" }
            }
        }
    }
}

private suspend fun CoroutineScope.circle(
    serverSocket: ServerSocket,
    engine: ServerSerializedEngine<ByteArray>,
    timeout: Duration,
) {
    val clientSocket = serverSocket.accept()
    launch {
        clientSocket.use { clientSocket ->
            engine.handle(
                clientAddress = clientSocket.localAddress.toString(),
                readRequest = {
                    clientSocket
                        .openReadChannel()
                        .readSizeWithBytes(
                            timeout = timeout,
                        )
                },
                writeResponse = { response ->
                    clientSocket
                        .openWriteChannel()
                        .writeSizeWithBytes(
                            bytes = response,
                            timeout = timeout,
                        )
                }
            )

        }
    }
}

private inline fun logError(
    error: Throwable,
    action: () -> String,
) {
    logger.w(error) { "Error while ${action()}" }
}
