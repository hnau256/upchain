package org.hnau.upchain.core.repository

import arrow.core.NonEmptyList
import org.hnau.upchain.core.Update

interface UpchainMediator {

    suspend fun append(
        updates: NonEmptyList<Update>,
    )

    suspend fun replace(
        updates: List<Update>,
    )
}