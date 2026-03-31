package org.hnau.upchain.sync.core.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object SyncConstants {

    val tcpTimeout: Duration = 30.seconds
}