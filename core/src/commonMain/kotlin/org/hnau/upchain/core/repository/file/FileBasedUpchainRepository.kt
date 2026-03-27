package org.hnau.upchain.core.repository.file

import arrow.core.NonEmptyList
import org.hnau.upchain.core.Update
import org.hnau.upchain.core.repository.UpchainMediator
import org.hnau.upchain.core.repository.UpchainRepository
import org.hnau.upchain.core.repository.create

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