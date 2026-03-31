package org.hnau.upchain.sync.http

import org.hnau.upchain.sync.core.ServerPort


private val serverPortDefaultHttp = ServerPort(
    port = 80,
)

val ServerPort.Companion.defaultHttp: ServerPort
    get() = serverPortDefaultHttp