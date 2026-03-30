package org.hnau.upchain.core

import kotlinx.serialization.Serializable
import org.hnau.commons.kotlin.mapper.Mapper
import org.hnau.commons.kotlin.mapper.plus
import org.hnau.commons.kotlin.mapper.stringToUuid
import org.hnau.commons.kotlin.serialization.UuidSerializer
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class UpchainId(
    @Serializable(UuidSerializer::class)
    val id: Uuid,
) {

    companion object {

        val uuidMapper: Mapper<Uuid, UpchainId> =
            Mapper(::UpchainId, UpchainId::id)

        val stringMapper: Mapper<String, UpchainId> =
            Mapper.stringToUuid + uuidMapper

        fun createRandom(): UpchainId = Uuid
            .random()
            .let(uuidMapper.direct)
    }
}