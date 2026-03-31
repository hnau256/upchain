package org.hnau.upchain.sync.client.tcp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class ServerAddress(
    val address: String,
)