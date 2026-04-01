package org.hnau.upchain.sync.server.http

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.origin
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.awaitCancellation
import org.hnau.upchain.sync.core.ServerPort
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.http.HttpScheme
import org.hnau.upchain.sync.http.JsonTransportMapper
import org.hnau.upchain.sync.http.SyncConstantsHttp
import org.hnau.upchain.sync.server.core.ServerSerializedEngine

suspend fun httpSyncServer(
    engine: SyncApi,
    port: ServerPort = HttpScheme.Http.port,
): Result<Nothing> = runCatching {

    val serializedEngine: ServerSerializedEngine<String> = ServerSerializedEngine(
        engine = engine,
        transportMapper = JsonTransportMapper,
    )

    val server = embeddedServer(
        factory = CIO,
        port = port.port,
    ) {
        routing {
            post(SyncConstantsHttp.route) {
                serializedEngine.handle(
                    clientAddress = call.request.origin.remoteAddress,
                    readRequest = call::receiveText,
                    writeResponse = call::respondText,
                )
            }
        }
    }

    try {
        server.start(wait = true)
        awaitCancellation()
    } finally {
        server.stop()
    }
}