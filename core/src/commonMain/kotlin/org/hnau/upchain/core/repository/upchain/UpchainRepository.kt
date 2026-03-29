package org.hnau.upchain.core.repository.upchain

import kotlinx.coroutines.flow.StateFlow
import org.hnau.upchain.core.Upchain

interface UpchainRepository {

    val upchain: StateFlow<Upchain>

    suspend fun setNewUpchain(
        currentUpchainToCheck: Upchain,
        newUpchain: Upchain,
    ): Boolean

    companion object
}