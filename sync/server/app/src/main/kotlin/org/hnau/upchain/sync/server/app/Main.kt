package org.hnau.upchain.sync.server.app

import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import kotlinx.coroutines.runBlocking
import org.hnau.commons.kotlin.foldNullable
import org.hnau.upchain.core.repository.file.upchains.fileBased
import org.hnau.upchain.core.repository.upchains.UpchainsRepository
import org.hnau.upchain.sync.core.ServerPort
import org.hnau.upchain.sync.server.core.ServerEngine
import org.hnau.upchain.sync.server.core.repository.toCreateOnly
import org.hnau.upchain.sync.server.http.httpSyncServer

fun main(args: Array<String>) {
    Logger.setLogWriters(platformLogWriter())

    val parser = ArgParser("Upchain server")

    val upchainsDir by parser
        .option(
            type = ArgType.String,
            shortName = "d",
        )
        .required()

    val port by parser.option(
        type = ArgType.Int,
        shortName = "p",
    )
    parser.parse(args)

    runBlocking {

        val repository = UpchainsRepository.fileBased(
            dir = upchainsDir,
        )

        val engine = ServerEngine(
            scope = this,
            repository = repository.toCreateOnly(this),
        )

        port
            ?.let(::ServerPort)
            .foldNullable(
                ifNull = {
                    httpSyncServer(
                        engine = engine,
                    )
                },
                ifNotNull = { port ->
                    httpSyncServer(
                        engine = engine,
                        port = port,
                    )
                }
            )

    }
}
