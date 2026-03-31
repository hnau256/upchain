package org.hnau.upchain.sync.http

import kotlinx.serialization.KSerializer
import org.hnau.commons.kotlin.mapper.Mapper
import org.hnau.upchain.sync.core.ApiResponse

fun <T> ApiResponse.Companion.createJsonMapper(
    dataSerializer: KSerializer<T>,
): Mapper<String, ApiResponse<T>> = Mapper(
    direct = {
        TODO()
        /*val head = it.first()
        val tail = it.drop(1).toByteArray()
        when (head) {
            zeroByte -> SyncConstantsHttp
                .cbor
                .decodeFromByteArray(
                    deserializer = String.serializer(),
                    bytes = tail,
                )
                .takeIf(String::isNotEmpty)
                .let(::Error)

            else -> SyncConstantsHttp
                .cbor
                .decodeFromByteArray(
                    deserializer = dataSerializer,
                    bytes = tail,
                )
                .let(::Success)
        }*/
    },
    reverse = { response ->
        TODO()
        /*val (head, tail) = when (response) {
            is Success -> oneByte to SyncConstantsHttp
                .cbor
                .encodeToByteArray(
                    serializer = dataSerializer,
                    value = response.data,
                )

            is Error -> zeroByte to SyncConstantsHttp
                .cbor
                .encodeToByteArray(
                    serializer = String.serializer(),
                    value = response.error.orEmpty(),
                )
        }
        ByteArray(tail.size + 1) { i ->
            when (i) {
                0 -> head
                else -> tail[i - 1]
            }
        }*/
    }
)

private const val zeroByte: Byte = 0
private const val oneByte: Byte = 1