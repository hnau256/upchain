package org.hnau.upchain.sync.core

import kotlinx.serialization.Serializable
import org.hnau.commons.kotlin.serialization.UuidSerializer
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class UpchainId(
    @Serializable(UuidSerializer::class)
    val id: Uuid,
)