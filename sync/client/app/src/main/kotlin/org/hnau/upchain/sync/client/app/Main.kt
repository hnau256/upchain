package org.hnau.upchain.sync.client.app

import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.hnau.commons.kotlin.coroutines.createChild
import org.hnau.commons.kotlin.foldBoolean
import org.hnau.commons.kotlin.foldNullable
import org.hnau.upchain.core.UpchainId
import org.hnau.upchain.core.repository.file.upchain.fileBased
import org.hnau.upchain.core.repository.upchain.UpchainRepository
import org.hnau.upchain.sync.client.app.utils.ServerHostArgType
import org.hnau.upchain.sync.client.core.SyncListener
import org.hnau.upchain.sync.client.core.sync
import org.hnau.upchain.sync.client.http.HttpSyncClient
import org.hnau.upchain.sync.core.ServerPort
import org.hnau.upchain.sync.http.HttpScheme

private val logger = Logger.withTag("Main")

fun main(args: Array<String>) {
    Logger.setLogWriters(platformLogWriter())

    val parser = ArgParser("Upchain client")

    val upchainsFile by parser
        .option(
            type = ArgType.String,
            shortName = "f",
        )
        .required()

    val host by parser
        .option(
            type = ServerHostArgType,
            shortName = "s",
        )
        .required()

    val port by parser.option(
        type = ArgType.Int,
        shortName = "p",
    )

    val useTls by parser
        .option(
            type = ArgType.Boolean,
            shortName = "tls",
        )
        .default(false)

    parser.parse(args)

    runBlocking {
        val repository =
            UpchainRepository.fileBased(
                filename = upchainsFile,
            )

        val clientScope = createChild()
        val scheme = useTls.foldBoolean(
            ifTrue = { HttpScheme.Https },
            ifFalse = { HttpScheme.Http },
        )

        val api = port
            ?.let(::ServerPort)
            .foldNullable(
                ifNull = {
                    HttpSyncClient(
                        scope = clientScope,
                        host = host,
                        scheme = scheme,
                    )
                },
                ifNotNull = { port ->
                    HttpSyncClient(
                        scope = clientScope,
                        host = host,
                        port = port,
                        scheme = scheme,
                    )
                }
            )

        repository
            .sync(
                id = upchainsFile
                    .split("/")
                    .last()
                    .let(UpchainId.stringMapper.direct),
                api = api,
                listener = SyncListener(
                    onUpdatesFromServers = { updatesCount ->
                        logger.i { "Received $updatesCount updates from server" }
                    },
                    onApplyServerUpdates = { updatesCount ->
                        logger.i { "Applied $updatesCount updates from server" }
                    },
                    onUpdatesToServer = { updatesCount ->
                        logger.i { "Pushed $updatesCount updates to server" }
                    }
                )
            )
            .getOrThrow()

        logger.i { "Synchronized" }

        clientScope.cancel()
    }
}
