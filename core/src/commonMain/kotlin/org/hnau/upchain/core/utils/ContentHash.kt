package org.hnau.upchain.core.utils

import org.hnau.commons.kotlin.castOrNull
import org.hnau.commons.kotlin.mapper.Mapper
import org.hnau.commons.kotlin.mapper.plus
import org.hnau.commons.kotlin.mapper.stringAsBase64ByteArray
import org.hnau.upchain.core.UpchainHash
import org.hnau.upchain.core.Update

class ContentHash private constructor(
    val hash: ByteArray,
) {

    override fun toString(): String =
        "ContentHash(${stringMapper.reverse(this)})"

    override fun equals(
        other: Any?,
    ): Boolean = other
        ?.castOrNull<UpchainHash>()
        ?.takeIf { hash.contentEquals(it.hash) } != null

    override fun hashCode(): Int = hash.contentHashCode()

    companion object {

        fun create(
            update: Update,
        ): ContentHash {
            val updateBytes = update.value.encodeToByteArray()
            val newHash = sha256(updateBytes)
            return ContentHash(
                hash = newHash,
            )
        }

        val stringMapper: Mapper<String, ContentHash> =
            Mapper.stringAsBase64ByteArray + Mapper(::ContentHash, ContentHash::hash)
    }
}