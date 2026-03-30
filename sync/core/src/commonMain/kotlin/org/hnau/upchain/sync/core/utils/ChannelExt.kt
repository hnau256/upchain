package org.hnau.upchain.sync.core.utils

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.readInt
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writeInt
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

suspend fun ByteReadChannel.readSizeWithBytes(
    timeout: Duration,
): ByteArray = withTimeout(timeout) {
    val size = readInt()
    readByteArray(size)
}

suspend fun ByteWriteChannel.writeSizeWithBytes(
    bytes: ByteArray,
    timeout: Duration,
) {
    withTimeout(timeout) {
        writeInt(bytes.size)
        writeByteArray(bytes)
        flush()
    }
}
