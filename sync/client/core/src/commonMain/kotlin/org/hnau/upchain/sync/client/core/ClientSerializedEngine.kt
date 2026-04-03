package org.hnau.upchain.sync.client.core

import kotlinx.coroutines.CancellationException
import org.hnau.commons.gen.loggable.annotations.Loggable
import org.hnau.upchain.sync.core.ApiResponse
import org.hnau.upchain.sync.core.SyncApi
import org.hnau.upchain.sync.core.SyncHandle
import org.hnau.upchain.sync.core.TransportMapperFactory

@Loggable
class ClientSerializedEngine<T>(
    private val serverAddress: String,
    private val transportMapperFactory: TransportMapperFactory<T>,
    private val doRequest: suspend (T) -> T,
) : SyncApi {

    override suspend fun <O, I : SyncHandle<O>> handle(
        request: I,
    ): Result<O> = try {

        logger.d { "Request to $serverAddress: $request" }

        val encodedRequest: T = transportMapperFactory
            .createTransportMapper(
                serializer = SyncHandle.serializer,
            )
            .reverse(request)

        val encodedResponse = doRequest(encodedRequest)

        val response = transportMapperFactory
            .createTransportMapper(
                serializer = ApiResponse.serializer(request.responseSerializer),
            )
            .direct(encodedResponse)

        logger.d { "Response from $serverAddress: $response" }

        when (response) {
            is ApiResponse.Success ->
                Result.success(response.data)

            is ApiResponse.Error ->
                Result.failure(Exception("Error received from sync server: ${response.error}"))
        }
    } catch (ex: CancellationException) {
        throw ex
    } catch (th: Throwable) {
        logger.d { "Error while executing request to $serverAddress: ${th.message}" }
        Result.failure(th)
    }

    companion object
}