package org.hnau.upchain.sync.tcp

import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import org.hnau.upchain.sync.core.TransportMapper

data object CborTransportMapper : TransportMapper<ByteArray> {

    override fun <I> decode(
        transport: ByteArray,
        serializer: KSerializer<I>
    ): I = cbor.decodeFromByteArray(
        bytes = transport,
        deserializer = serializer,
    )

    override fun <O> encode(
        output: O,
        serializer: KSerializer<O>
    ): ByteArray = cbor.encodeToByteArray(
        value = output,
        serializer = serializer,
    )

    private val cbor: Cbor = Cbor {
        encodeDefaults = false
        ignoreUnknownKeys = true
    }
}