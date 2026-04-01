package org.hnau.upchain.sync.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ApiResponse<out T> {

    @Serializable
    @SerialName("success")
    data class Success<out T>(
        val data: T,
    ) : ApiResponse<T>

    @Serializable
    @SerialName("error")
    data class Error(
        val error: String?,
    ) : ApiResponse<Nothing>

    companion object
}