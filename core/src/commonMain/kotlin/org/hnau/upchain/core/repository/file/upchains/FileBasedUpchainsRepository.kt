package org.hnau.upchain.core.repository.file.upchains

import org.hnau.upchain.core.UpchainId
import org.hnau.upchain.core.repository.file.upchain.fileBased
import org.hnau.upchain.core.repository.upchain.UpchainRepository
import org.hnau.upchain.core.repository.upchains.UpchainsRepository
import org.hnau.upchain.core.repository.upchains.create

suspend fun UpchainsRepository.Companion.fileBased(
    dir: String,
): UpchainsRepository {

    val dirAccessor = DirAccessor.create(
        dir = dir,
    )

    val ids = dirAccessor
        .readFiles()
        .map(UpchainId.stringMapper.direct)

    fun UpchainId.toFilename(): String =
        let(UpchainId.stringMapper.reverse)

    return UpchainsRepository.create(
        ids = ids,
        createRepository = { id ->
            UpchainRepository.fileBased(
                filename = id
                    .toFilename()
                    .let(dirAccessor::createAbsoluteFileName)
            )
        },
        createUpchain = { id ->
            id
                .toFilename()
                .let { filename -> dirAccessor.addFile(filename) }
        },
        removeUpchain = { id ->
            id
                .toFilename()
                .let { filename -> dirAccessor.removeFile(filename) }
        }
    )
}