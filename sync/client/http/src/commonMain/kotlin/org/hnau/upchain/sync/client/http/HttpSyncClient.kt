package org.hnau.upchain.sync.client.http

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.hnau.upchain.sync.client.core.ClientSerializedEngine
import org.hnau.upchain.sync.core.ServerHost
import org.hnau.upchain.sync.core.ServerPort
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.core.SyncHandle
import org.hnau.upchain.sync.http.HttpScheme
import org.hnau.upchain.sync.http.JsonTransportMapper
import org.hnau.upchain.sync.http.SyncConstantsHttp

class HttpSyncClient(
    scope: CoroutineScope,
    private val host: ServerHost,
    scheme: HttpScheme = HttpScheme.default,
    port: ServerPort = scheme.port,
) : SyncApi {

    private val client: HttpClient = HttpClient()

    private val serialized: ClientSerializedEngine<String> = run {

        val url = run {
            val schemeString = when (scheme) {
                HttpScheme.Http -> "http"
                HttpScheme.Https -> "https"
            }
            "$schemeString://${host.host}:${port.port}${SyncConstantsHttp.route}"
        }

        ClientSerializedEngine(
            serverAddress = url,
            transportMapper = JsonTransportMapper,
        ) { request ->
            client
                .post(url) {
                    setBody(
                        TextContent(
                            text = request,
                            contentType = ContentType.Application.Json,
                        ),
                    )
                }
                .body()
        }
    }

    init {
        scope.launch {
            try {
                awaitCancellation()
            } catch (ex: CancellationException) {
                client.close()
                throw ex
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <O, I : SyncHandle<O>> handle(
        request: I,
    ): Result<O> = serialized.handle(
        request = request,
    )
}
