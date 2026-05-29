package org.hnau.upchain.core.utils

import org.hnau.commons.kotlin.castOrThrow
import java.security.MessageDigest


private val digestPrototype: MessageDigest = MessageDigest
    .getInstance("SHA-256")

internal actual fun sha256(
    input: ByteArray,
): ByteArray = digestPrototype
    .clone()
    .castOrThrow<MessageDigest>()
    .digest(input)