package org.hnau.upchain.core.repository.file.upchain

import arrow.core.NonEmptyList
import org.hnau.upchain.core.Update
import org.hnau.upchain.core.repository.upchain.UpchainMediator
import org.hnau.upchain.core.repository.upchain.UpchainRepository
import org.hnau.upchain.core.repository.upchain.create

suspend fun UpchainRepository.Companion.fileBased(
    filename: String,
): UpchainRepository {

    val accessor = FileAccessor.create(
        filename = filename,
    )

    return UpchainRepository.create(
        updates = accessor
            .readLines()
            .map(::Update),

        mediator = object : UpchainMediator {

            override suspend fun append(
                updates: NonEmptyList<Update>,
            ) {
                accessor.appendLines(
                    lines = updates.map(Update::value)
                )
            }

            override suspend fun replace(
                updates: List<Update>,
            ) {
                accessor.replaceLines(
                    lines = updates.map(Update::value)
                )
            }
        }
    )
}