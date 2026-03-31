package org.hnau.upchain.sync.server.http

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.ExperimentalSerializationApi
import org.hnau.upchain.sync.core.ApiResponse
import org.hnau.upchain.sync.core.ServerPort
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.core.SyncHandle
import org.hnau.upchain.sync.http.SyncConstantsHttp
import org.hnau.upchain.sync.http.createJsonMapper
import org.hnau.upchain.sync.http.defaultHttp

suspend fun httpSyncServer(
    api: SyncApi,
    port: ServerPort = ServerPort.defaultHttp,
): Result<Nothing> = runCatching {
    val server = embeddedServer(
        factory = CIO,
        port = port.port,
    ) {
        configureServer(api)
    }

    try {
        server.start(wait = true)
        awaitCancellation()
    } finally {
        server.stop()
    }
}

private fun Application.configureServer(
    api: SyncApi,
) {

    install(ContentNegotiation) {
        json(SyncConstantsHttp.json)
    }

    routing {
        post("/") {
            val request = call.receiveText()
            val response = api.handle(request)
            call.respondText(response)
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
        ApiResponse
            .createJsonMapper(request.responseSerializer)
            .reverse(response)
    }
