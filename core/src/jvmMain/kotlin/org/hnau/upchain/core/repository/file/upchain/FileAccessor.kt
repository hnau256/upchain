package org.hnau.upchain.core.repository.file.upchain

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

        val lines = mutableListOf<String>()
        var index = 0
        while (index < text.length) {
            val nextNewline = text.indexOf('\n', index)
            if (nextNewline == -1) break
            val line = text.substring(index, nextNewline)
            if (line.isNotEmpty()) {
                lines.add(line)
            }
            index = nextNewline + 1
        }

        if (index != text.length) {
            writeAtomically(lines)
        }

        return lines
    }

    override suspend fun appendLines(
        lines: NonEmptyList<String>,
    ) {
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

    override suspend fun replaceLines(
        lines: List<String>,
    ) {
        writeAtomically(lines)
    }

    private fun writeAtomically(
        lines: List<String>,
    ) {
        path.parent?.let { parent ->
            if (!parent.exists()) {
                parent.createDirectories()
            }
        }
        val tempPath = Files.createTempFile(
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
