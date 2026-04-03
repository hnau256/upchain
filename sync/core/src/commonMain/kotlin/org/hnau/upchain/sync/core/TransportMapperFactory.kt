package org.hnau.upchain.sync.core

import kotlinx.serialization.KSerializer
import org.hnau.commons.kotlin.mapper.Mapper

interface TransportMapperFactory<T> {

    fun <V> createTransportMapper(
        serializer: KSerializer<V>,
    ): Mapper<T, V>
}