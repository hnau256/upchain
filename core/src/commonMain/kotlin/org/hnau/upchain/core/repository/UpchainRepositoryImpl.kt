package org.hnau.upchain.core.repository

import arrow.core.toNonEmptyListOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import org.hnau.upchain.core.Upchain
import org.hnau.upchain.core.Update
import org.hnau.upchain.core.getUpdatesAfterHashIfPossible
import org.hnau.upchain.core.plus

private class UpchainRepositoryImpl(
    initialUpchain: Upchain,
    private val mediator: UpchainMediator,
) : UpchainRepository {

    private val _upchain: MutableStateFlow<Upchain> =
        initialUpchain.toMutableStateFlowAsInitial()

    override val upchain: StateFlow<Upchain>
        get() = _upchain


    private val accessUpchainMutex = Mutex()

    override suspend fun setNewUpchain(
        currentUpchainToCheck: Upchain,
        newUpchain: Upchain
    ): Boolean = accessUpchainMutex.withLock {
        val currentUpchain = _upchain.value
        if (currentUpchainToCheck != currentUpchain) {
            return@withLock false
        }
        val updates = newUpchain.getUpdatesAfterHashIfPossible(
            hash = currentUpchain.peekHash,
        )
        when (updates) {
            null -> mediator.replace(
                updates = newUpchain.items.map(Upchain.Item::update),
            )

            else -> updates.toNonEmptyListOrNull()?.let { nonEmptyUpdates ->
                mediator.append(
                    updates = nonEmptyUpdates,
                )
            }

        }
        _upchain.value = newUpchain
        true
    }
}

suspend fun UpchainRepository.Companion.create(
    updates: Sequence<Update>,
    mediator: UpchainMediator,
): UpchainRepository {
    val initialUpchain: Upchain = withContext(Dispatchers.Default) {
        Upchain.empty + updates
    }
    return UpchainRepositoryImpl(
        initialUpchain = initialUpchain,
        mediator = mediator,
    )
}