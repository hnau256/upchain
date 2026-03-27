package org.hnau.upchain.core.repository.file

import arrow.core.NonEmptyList

interface FileAccessor {

    suspend fun readLines(): Sequence<String>

    suspend fun appendLines(
        lines: NonEmptyList<String>,
    )

    suspend fun replaceLines(
        lines: List<String>,
    )

    companion object
}

internal expect fun FileAccessor.Companion.create(
    filename: String,
): FileAccessor