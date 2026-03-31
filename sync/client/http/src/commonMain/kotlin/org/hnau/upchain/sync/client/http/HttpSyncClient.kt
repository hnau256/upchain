package org.hnau.upchain.sync.client.http

import arrow.core.flatMap
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.core.SyncHandle

class HttpSyncClient(
    scope: CoroutineScope,
    private val baseUrl: String,
) : SyncApi {

    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = false
                    isLenient = true
                    ignoreUnknownKeys = true
                },
            )
        }
    }

    private val json: Json = Json {
        prettyPrint = false
        isLenient = true
        ignoreUnknownKeys = true
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
    ): Result<O> = runCatching {

        val requestJson: String = json.encodeToString(
            serializer = SyncHandle.serializer,
            value = request as SyncHandle<*>,
        )

        val responseJson: JsonElement = client
            .post(baseUrl) {
                contentType(ContentType.Application.Json)
                setBody(
                    TextContent(
                        text = requestJson,
                        contentType = ContentType.Application.Json,
                    ),
                )
            }
            .body()

        responseJson
    }.flatMap { responseJson ->
        val jsonObject = responseJson.jsonObject
        val type = jsonObject["type"]?.jsonPrimitive?.content

        when (type) {
            "success" -> {
                val dataJson: JsonElement = jsonObject["data"] ?: return@flatMap Result.failure(
                    Exception("Missing data field in success response"),
                )
                val data: O = json.decodeFromJsonElement(
                    deserializer = request.responseSerializer,
                    element = dataJson,
                )
                Result.success(data)
            }

            "error" -> {
                val errorMessage = jsonObject["error"]?.jsonPrimitive?.content
                Result.failure(
                    Exception("Error received from sync server: $errorMessage"),
                )
            }

            else -> Result.failure(
                Exception("Unknown response type: $type"),
            )
        }
    }
}
