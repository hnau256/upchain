package org.hnau.upchain.sync.http

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.hnau.commons.kotlin.mapper.Mapper
import org.hnau.commons.kotlin.mapper.toMapper
import org.hnau.upchain.sync.core.TransportMapperFactory

data object JsonTransportMapperFactory : TransportMapperFactory<String> {

    override fun <V> createTransportMapper(
        serializer: KSerializer<V>,
    ): Mapper<String, V> = json.toMapper(
        serializer = serializer,
    )

    private val json: Json = Json {
        prettyPrint = false
        isLenient = true
        ignoreUnknownKeys = true
    }
}