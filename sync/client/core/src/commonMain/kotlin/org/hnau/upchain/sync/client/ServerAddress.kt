package org.hnau.upchain.sync.client

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class ServerAddress(
    val address: String,
)