package org.hnau.upchain.sync.core.utils

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.readInt
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writeInt

suspend fun ByteReadChannel.readSizeWithBytes(): ByteArray {
    val size = readInt()
    return readByteArray(size)
}

suspend fun ByteWriteChannel.writeSizeWithBytes(
    bytes: ByteArray,
) {
    writeInt(bytes.size)
    writeByteArray(bytes)
    flush()
}