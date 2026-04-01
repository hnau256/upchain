package org.hnau.upchain.sync.server.http

import co.touchlab.kermit.Logger
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.origin
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.ExperimentalSerializationApi
import org.hnau.upchain.sync.core.ApiResponse
import org.hnau.upchain.sync.core.ServerPort
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.core.SyncHandle
import org.hnau.upchain.sync.http.HttpScheme
import org.hnau.upchain.sync.http.SyncConstantsHttp
import org.hnau.upchain.sync.http.encodeToJson

private val logger = Logger.withTag("HttpSyncServer")

suspend fun httpSyncServer(
    engine: SyncApi,
    port: ServerPort = HttpScheme.Http.port,
): Result<Nothing> = runCatching {

    val server = embeddedServer(
        factory = CIO,
        port = port.port,
    ) {
        configureServer(engine)
    }

    try {
        server.start(wait = true)
        awaitCancellation()
    } finally {
        server.stop()
    }
}

private fun Application.configureServer(
    engine: SyncApi,
) {
    routing {
        post(SyncConstantsHttp.route) {
            val clientAddress = call.request.origin.remoteAddress
            try {
                val request = call.receiveText()
                logger.d { "Request from $clientAddress: $request" }
                val response = engine.handle(request)
                logger.d { "Response to $clientAddress: $response" }
                call.respondText(response)
            } catch (ex: CancellationException) {
                throw ex
            } catch (th: Throwable) {
                val response = ApiResponse.Error(th.message).encodeToJson()
                logger.w(th) { "Error response to $clientAddress: $response" }
                call.respondText(response)
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
private suspend fun SyncApi.handle(
    request: String,
): String {
    val typedRequest = SyncConstantsHttp.json.decodeFromString(
        SyncHandle.serializer,
        request,
    )
    return handleTyped(typedRequest)
}

private suspend fun <O, I : SyncHandle<O>> SyncApi.handleTyped(
    request: I,
): String = handle(request)
    .fold(
        onSuccess = { result -> ApiResponse.Success(result) },
        onFailure = { error -> ApiResponse.Error(error.message) },
    )
    .let { response ->
        SyncConstantsHttp.json.encodeToString(
            serializer = ApiResponse.serializer(request.responseSerializer),
            value = response,
        )
    }
