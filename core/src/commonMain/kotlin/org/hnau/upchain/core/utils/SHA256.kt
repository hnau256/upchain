package org.hnau.upchain.core.utils


internal expect fun sha256(
    input: ByteArray,
): ByteArray