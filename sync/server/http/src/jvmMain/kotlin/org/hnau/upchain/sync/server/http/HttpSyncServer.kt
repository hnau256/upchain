package org.hnau.upchain.sync.server.http

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.Json
import org.hnau.upchain.sync.core.ApiResponse
import org.hnau.upchain.sync.core.ServerPort
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.core.SyncHandle

suspend fun httpSyncServer(
    api: SyncApi,
    port: ServerPort = ServerPort(80),
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
        json(
            Json {
                prettyPrint = false
                isLenient = true
                ignoreUnknownKeys = true
            },
        )
    }

    routing {
        post("/") {
            val request = call.receive<SyncHandle<*>>()
            val response =
                handleRequest(
                    api = api,
                    request = request,
                )
            call.respond(response)
        }
    }
}

private suspend fun handleRequest(
    api: SyncApi,
    request: SyncHandle<*>,
): ApiResponse<*> = try {
    val result = api.handle(request)
    result.fold(
        onSuccess = { data -> ApiResponse.Success(data) },
        onFailure = { error -> ApiResponse.Error(error.message) },
    )
} catch (ex: CancellationException) {
    throw ex
} catch (th: Throwable) {
    ApiResponse.Error(th.message)
}
