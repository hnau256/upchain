package org.hnau.upchain.core.repository.file

import arrow.core.NonEmptyList
import org.hnau.commons.kotlin.ifNull
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension

internal actual fun FileAccessor.Companion.create(filename: String): FileAccessor =
    FileAccessorImpl(
        path = Path.of(filename),
    )

private class FileAccessorImpl(
    private val path: Path,
) : FileAccessor {
    override suspend fun readLines(): Iterable<String> {

        val text = path
            .takeIf(Path::exists)
            ?.let(Files::readAllBytes)
            .ifNull { byteArrayOf() }
            .decodeToString()

        if (text.endsWith('\n')) {
            return text
                .lineSequence()
                .filter { it.isNotEmpty() }
                .toList()
        }

        val lastNewlineIndex = text.lastIndexOf('\n')
        val validLines = text
            .substring(0, lastNewlineIndex + 1)
            .lineSequence()
            .filter { it.isNotEmpty() }
            .toList()

        writeAtomically(validLines)

        return validLines
    }

    override suspend fun appendLines(lines: NonEmptyList<String>) {
        Files.write(
            path,
            lines
                .map { "$it\n" }
                .joinToString(separator = "")
                .encodeToByteArray(),
            StandardOpenOption.APPEND,
            StandardOpenOption.CREATE,
        )
    }

    override suspend fun replaceLines(lines: List<String>) {
        writeAtomically(lines)
    }

    private fun writeAtomically(lines: List<String>) {
        path.parent?.let { parent ->
            if (!parent.exists()) {
                parent.createDirectories()
            }
        }
        val tempPath =
            Files.createTempFile(
                path.parent ?: Path.of("."),
                path.nameWithoutExtension,
                ".tmp",
            )
        try {
            Files.write(tempPath, lines)
            Files.move(
                tempPath,
                path,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (e: Exception) {
            Files.deleteIfExists(tempPath)
            throw e
        }
    }
}
