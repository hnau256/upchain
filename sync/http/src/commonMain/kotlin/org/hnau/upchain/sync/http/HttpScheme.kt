package org.hnau.upchain.sync.http

import org.hnau.upchain.sync.core.ServerPort

enum class HttpScheme(
    val port: ServerPort,
) {
    Http(
        port = ServerPort(80),
    ),
    Https(
        port = ServerPort(443),
    );

    companion object {

        val default: HttpScheme
            get() = Https
    }
}