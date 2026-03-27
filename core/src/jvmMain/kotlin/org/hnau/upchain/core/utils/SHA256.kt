package org.hnau.upchain.core.utils

import java.security.MessageDigest


private val digest: MessageDigest = MessageDigest
    .getInstance("SHA-256")

internal actual fun sha256(
    input: ByteArray,
): ByteArray = digest
    .digest(input)