package org.hnau.upchain.sync.core

import io.ktor.http.DEFAULT_PORT
import io.ktor.http.Url
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class ServerHost private constructor(
    val host: String,
) {

    companion object {

        fun createOrNull(
            input: String,
        ): ServerHost? = input
            .trim()
            .takeIf(String::isNotBlank)
            ?.takeIf(::isCorrectHost)
            ?.let(::ServerHost)

        private fun isCorrectHost(
            input: String,
        ): Boolean = try {
            Url("http://$this").run {
                host.isNotEmpty() &&
                        encodedPath.isEmpty() &&
                        specifiedPort == DEFAULT_PORT &&
                        parameters.isEmpty() &&
                        fragment.isEmpty() &&
                        user.orEmpty().isEmpty() &&
                        password.orEmpty().isEmpty() &&
                        (host == input || "[$host]" == input)
            }
        } catch (_: Throwable) {
            false
        }
    }
}