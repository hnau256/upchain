package org.hnau.upchain.sync.tcp

import org.hnau.upchain.sync.core.ServerPort


private val serverPortDefaultTcp = ServerPort(
    port = 26385,
)

val ServerPort.Companion.defaultTcp: ServerPort
    get() = serverPortDefaultTcp