package org.hnau.upchain.sync.server.core

import kotlinx.coroutines.CancellationException
import org.hnau.commons.gen.loggable.annotations.Loggable
import org.hnau.upchain.sync.core.ApiResponse
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.core.SyncHandle
import org.hnau.upchain.sync.core.TransportMapper


@Loggable
class ServerSerializedEngine<T>(
    private val engine: SyncApi,
    private val transportMapper: TransportMapper<T>,
) {

    suspend fun handle(
        clientAddress: String,
        readRequest: suspend () -> T,
        writeResponse: suspend (T) -> Unit,
    ) {
        try {
            val encodedRequest = readRequest()
            val request = transportMapper.decode(
                transport = encodedRequest,
                serializer = SyncHandle.serializer,
            )
            logger.d { "Request from $clientAddress: $request" }
            val (response, encodedResponse) = handleTyped(request)
            writeResponse(encodedResponse)
            logger.d { "Response to $clientAddress: $response" }
        } catch (ex: CancellationException) {
            throw ex
        } catch (th: Throwable) {
            val response = ApiResponse.Error(th.message)
            logger.w(th) { "Error response to $clientAddress: $response" }
            transportMapper.encode(
                output = response,
                serializer = ApiResponse.Error.serializer(),
            )
        }
    }

    private suspend fun <O, I : SyncHandle<O>> handleTyped(
        request: I,
    ): Pair<ApiResponse<O>, T> = engine
        .handle(request)
        .fold(
            onSuccess = { result -> ApiResponse.Success(result) },
            onFailure = { error -> ApiResponse.Error(error.message) },
        )
        .let { response ->
            val encodedResponse = transportMapper.encode(
                output = response,
                serializer = ApiResponse.serializer(request.responseSerializer),
            )
            response to encodedResponse
        }

    companion object
}