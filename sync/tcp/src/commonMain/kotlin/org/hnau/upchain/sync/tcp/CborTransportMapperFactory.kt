package org.hnau.upchain.sync.tcp

import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import org.hnau.commons.kotlin.mapper.Mapper
import org.hnau.commons.kotlin.mapper.toMapper
import org.hnau.upchain.sync.core.TransportMapperFactory

data object CborTransportMapperFactory : TransportMapperFactory<ByteArray> {

    override fun <V> createTransportMapper(
        serializer: KSerializer<V>,
    ): Mapper<ByteArray, V> = cbor.toMapper(
        serializer = serializer,
    )

    private val cbor: Cbor = Cbor {
        encodeDefaults = false
        ignoreUnknownKeys = true
    }
}