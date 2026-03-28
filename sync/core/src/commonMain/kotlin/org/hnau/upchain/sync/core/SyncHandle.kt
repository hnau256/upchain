package org.hnau.upchain.sync.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.hnau.upchain.core.Upchain
import org.hnau.upchain.core.UpchainHash
import org.hnau.upchain.core.Update

@Serializable
sealed interface SyncHandle<O> {

    val responseSerializer: KSerializer<O>

    @Serializable
    @SerialName("get_upchains")
    data object GetUpchains : SyncHandle<GetUpchains.Response> {

        override val responseSerializer: KSerializer<Response>
            get() = Response.serializer()

        @Serializable
        data class Response(
            val upchains: List<Upchain>,
        ) {

            @Serializable
            data class Upchain(
                val id: UpchainId,
                val peekHash: UpchainHash?,
            )
        }
    }

    @Serializable
    @SerialName("get_updates")
    data class GetMaxToMinUpdates(
        val upchainId: UpchainId,
        val before: UpchainHash?,
    ) : SyncHandle<GetMaxToMinUpdates.Response> {

        override val responseSerializer: KSerializer<Response>
            get() = Response.serializer()

        @Serializable
        data class Response(
            val updates: List<Upchain.Item>,
            val hasMoreUpdates: Boolean,
        )
    }

    @Serializable
    @SerialName("append_updates")
    data class AppendUpdates(
        val upchainId: UpchainId,
        val peekHashToCheck: UpchainHash?,
        val updates: List<Update>,
    ) : SyncHandle<AppendUpdates.Response> {

        override val responseSerializer: KSerializer<Response>
            get() = Response.serializer()

        @Serializable
        data object Response
    }

    companion object {

        @Suppress("UNCHECKED_CAST")
        val serializer: KSerializer<SyncHandle<*>> = serializer(
            typeSerial0 = Unit.serializer(),
        ) as KSerializer<SyncHandle<*>>
    }
}