package org.hnau.upchain.sync.tcp

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import org.hnau.commons.kotlin.mapper.Mapper
import org.hnau.upchain.sync.core.ApiResponse
import org.hnau.upchain.sync.core.ApiResponse.Error
import org.hnau.upchain.sync.core.ApiResponse.Success

fun <T> ApiResponse.Companion.createCborMapper(
    dataSerializer: KSerializer<T>,
): Mapper<ByteArray, ApiResponse<T>> = Mapper(
    direct = {
        val head = it.first()
        val tail = it.drop(1).toByteArray()
        when (head) {
            zeroByte -> SyncConstantsTcp
                .cbor
                .decodeFromByteArray(
                    deserializer = String.serializer(),
                    bytes = tail,
                )
                .takeIf(String::isNotEmpty)
                .let(::Error)

            else -> SyncConstantsTcp
                .cbor
                .decodeFromByteArray(
                    deserializer = dataSerializer,
                    bytes = tail,
                )
                .let(::Success)
        }
    },
    reverse = { response ->
        val (head, tail) = when (response) {
            is Success -> oneByte to SyncConstantsTcp
                .cbor
                .encodeToByteArray(
                    serializer = dataSerializer,
                    value = response.data,
                )

            is Error -> zeroByte to SyncConstantsTcp
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
        }
    }
)

private const val zeroByte: Byte = 0
private const val oneByte: Byte = 1