package org.hnau.upchain.sync.client.core

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class ServerAddress(
    val address: String,
)