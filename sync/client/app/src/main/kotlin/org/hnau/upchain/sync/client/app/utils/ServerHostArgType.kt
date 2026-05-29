package org.hnau.upchain.sync.client.app.utils

import kotlinx.cli.ArgType
import kotlinx.cli.ParsingException
import org.hnau.upchain.sync.core.ServerHost

internal data object ServerHostArgType : ArgType<ServerHost>(
    hasParameter = true,
) {
    override val description: kotlin.String = "Server host"

    override fun convert(
        value: kotlin.String,
        name: kotlin.String
    ): ServerHost = ServerHost
        .createOrNull(
            input = value,
        )
        ?: throw ParsingException("Option $name is expected to be host name. $value is provided.")
}