package org.hnau.upchain.core.repository.file.upchains

import java.io.File

internal actual fun DirAccessor.Companion.create(
    dir: String,
): DirAccessor = object : DirAccessor {

    private val dirFile: File = File(dir)
        .apply { mkdirs() }

    private fun createAbsoluteFile(
        filename: String,
    ): File = dirFile.resolve(filename)

    override fun createAbsoluteFileName(
        filename: String,
    ): String = createAbsoluteFile(
        filename = filename,
    ).absolutePath

    override suspend fun readFiles(): List<String> =
        dirFile.list().orEmpty().toList()

    override suspend fun addFile(
        filename: String,
    ) {

    }

    override suspend fun removeFile(filename: String) {
        createAbsoluteFile(filename)
            .delete()
    }
}