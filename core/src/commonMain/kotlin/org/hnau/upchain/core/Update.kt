package org.hnau.upchain.core

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class Update(
    val value: String,
)