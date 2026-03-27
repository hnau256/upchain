package org.hnau.upchain.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.hnau.commons.kotlin.castOrNull
import org.hnau.commons.kotlin.mapper.Mapper
import org.hnau.commons.kotlin.mapper.plus
import org.hnau.commons.kotlin.mapper.stringAsBase64ByteArray
import org.hnau.commons.kotlin.serialization.MappingKSerializer
import org.hnau.upchain.core.utils.sha256

@Serializable(UpchainHash.Serializer::class)
class UpchainHash private constructor(
    val hash: ByteArray,
) {

    override fun toString(): String =
        "UpchainHash(${stringMapper.reverse(this)})"

    override fun equals(
        other: Any?,
    ): Boolean = other
        ?.castOrNull<UpchainHash>()
        ?.takeIf { hash.contentEquals(it.hash) } != null

    override fun hashCode(): Int = hash.contentHashCode()

    object Serializer : MappingKSerializer<String, UpchainHash>(
        base = String.serializer(),
        mapper = stringMapper,
    )

    companion object {

        fun create(
            previous: UpchainHash?,
            update: Update,
        ): UpchainHash {
            val updateBytes = update.value.encodeToByteArray()
            val hashWithUpdateBytes = when (previous) {
                null -> updateBytes
                else -> previous.hash + updateBytes
            }
            val newHash = sha256(hashWithUpdateBytes)
            return UpchainHash(
                hash = newHash,
            )
        }

        val stringMapper: Mapper<String, UpchainHash> =
            Mapper.stringAsBase64ByteArray + Mapper(::UpchainHash, UpchainHash::hash)
    }
}


fun UpchainHash?.calcNext(
    update: Update,
): UpchainHash = UpchainHash.create(
    previous = this,
    update = update,
)