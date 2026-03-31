package org.hnau.upchain.sync.http

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import org.hnau.commons.kotlin.mapper.Mapper
import org.hnau.upchain.sync.core.ApiResponse
import org.hnau.upchain.sync.core.ApiResponse.Error
import org.hnau.upchain.sync.core.ApiResponse.Success

fun <T> ApiResponse.Companion.createJsonMapper(
    dataSerializer: KSerializer<T>,
): Mapper<String, ApiResponse<T>> = Mapper(
    direct = {
        val head = it.first()
        val tail = it.drop(1)
        when (head) {
            errorSign -> SyncConstantsHttp
                .json
                .decodeFromString(
                    deserializer = String.serializer(),
                    string = tail,
                )
                .takeIf(String::isNotEmpty)
                .let(::Error)

            else -> SyncConstantsHttp
                .json
                .decodeFromString(
                    deserializer = dataSerializer,
                    string = tail,
                )
                .let(::Success)
        }
    },
    reverse = { response ->
        val (head, tail) = when (response) {
            is Success -> successSign to SyncConstantsHttp
                .json
                .encodeToString(
                    serializer = dataSerializer,
                    value = response.data,
                )

            is Error -> errorSign to SyncConstantsHttp
                .json
                .encodeToString(
                    serializer = String.serializer(),
                    value = response.error.orEmpty(),
                )
        }
        "$head$tail"
    }
)

private const val errorSign = '-'
private const val successSign = '+'