package org.hnau.upchain.sync.core

import kotlinx.serialization.KSerializer

interface TransportMapper<T> {

    fun <I> decode(
        transport: T,
        serializer: KSerializer<I>,
    ): I

    fun <O> encode(
        output: O,
        serializer: KSerializer<O>,
    ): T
}