package org.hnau.upchain.sync.core.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor

object SyncConstants {

    @OptIn(ExperimentalSerializationApi::class)
    val cbor: Cbor = Cbor
}