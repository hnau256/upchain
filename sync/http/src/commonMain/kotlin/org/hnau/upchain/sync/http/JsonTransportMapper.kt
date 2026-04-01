package org.hnau.upchain.sync.http

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.hnau.upchain.sync.core.TransportMapper

data object JsonTransportMapper : TransportMapper<String> {

    override fun <I> decode(
        transport: String,
        serializer: KSerializer<I>
    ): I = json.decodeFromString(
        string = transport,
        deserializer = serializer,
    )

    override fun <O> encode(
        output: O,
        serializer: KSerializer<O>
    ): String = json.encodeToString(
        value = output,
        serializer = serializer,
    )

    private val json: Json = Json {
        prettyPrint = false
        isLenient = true
        ignoreUnknownKeys = true
    }
}