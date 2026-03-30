package org.hnau.upchain.core.repository.upchain

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

    override suspend fun <R> editWithResult(
        modify: suspend (Upchain) -> Pair<Upchain, R>,
    ): R = accessUpchainMutex.withLock {
        val current = _upchain.value
        val (modified, result) = modify(current)

        val updates = modified.getUpdatesAfterHashIfPossible(
            hash = current.peekHash,
        )
        when (updates) {
            null -> mediator.replace(
                updates = modified
                    .items
                    .map(Upchain.Item::update),
            )

            else -> updates
                .toNonEmptyListOrNull()
                ?.let { nonEmptyUpdates ->
                    mediator.append(updates = nonEmptyUpdates)
                }

        }
        _upchain.value = modified
        result
    }
}

suspend fun UpchainRepository.Companion.create(
    updates: Iterable<Update>,
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