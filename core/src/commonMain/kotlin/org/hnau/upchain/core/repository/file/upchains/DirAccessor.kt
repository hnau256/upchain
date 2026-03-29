package org.hnau.upchain.core.repository.file.upchains

interface DirAccessor {
    
    fun createAbsoluteFileName(
        filename: String,
    ): String
    
    suspend fun readFiles(): List<String>
    
    suspend fun addFile(
        filename: String,
    )

    suspend fun removeFile(
        filename: String,
    )

    companion object
}

internal expect fun DirAccessor.Companion.create(
    dir: String,
): DirAccessor