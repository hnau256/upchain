package org.hnau.upchain.sync.client.http

import arrow.core.flatMap
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hnau.upchain.sync.core.ApiResponse
import org.hnau.upchain.sync.core.ServerAddress
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.core.SyncHandle
import org.hnau.upchain.sync.http.HttpScheme
import org.hnau.upchain.sync.http.SyncConstantsHttp
import org.hnau.upchain.sync.http.createJsonMapper

class HttpSyncClient(
    scope: CoroutineScope,
    private val address: ServerAddress,
    scheme: HttpScheme = HttpScheme.default,
) : SyncApi {

    private val url = run {
        val schemeString = when (scheme) {
            HttpScheme.Http -> "http"
            HttpScheme.Https -> "https"
        }
        "$schemeString://${address.address}:${scheme.port.port}${SyncConstantsHttp.route}"
    }

    private val client: HttpClient = HttpClient()

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
    ): Result<O> = runCatching {

        val requestJson: String = SyncConstantsHttp.json.encodeToString(
            serializer = SyncHandle.serializer,
            value = request as SyncHandle<*>,
        )

        val responseJson: String = client
            .post(url) {
                contentType(ContentType.Application.Json)
                setBody(
                    TextContent(
                        text = requestJson,
                        contentType = ContentType.Application.Json,
                    ),
                )
            }
            .body()

        val response = withContext(Dispatchers.Default) {
            ApiResponse
                .createJsonMapper(
                    dataSerializer = request.responseSerializer,
                )
                .direct(responseJson)
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
