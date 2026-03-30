package org.hnau.upchain.core.repository.upchains

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.hnau.commons.kotlin.KeyValue
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import org.hnau.upchain.core.UpchainId
import org.hnau.upchain.core.repository.upchain.UpchainRepository

internal suspend fun UpchainsRepository.Companion.create(
    ids: List<UpchainId>,
    createRepository: suspend (id: UpchainId) -> UpchainRepository,
    createUpchain: suspend (id: UpchainId) -> Unit,
    removeUpchain: suspend (id: UpchainId) -> Unit,
): UpchainsRepository {

    val initialRepositories: List<KeyValue<UpchainId, UpchainRepository>> = ids.map { id ->
        val repository = createRepository(id)
        KeyValue(
            key = id,
            value = repository,
        )
    }

    return object : UpchainsRepository {

        override val upchains: MutableStateFlow<List<UpchainsRepository.Item>> = initialRepositories
            .map { upchain -> upchain.toItem() }
            .toMutableStateFlowAsInitial()

        override suspend fun createUpchain(
            id: UpchainId,
        ) {
            update { currentList ->
                if (currentList.any { it.id == id }) {
                    return@update currentList
                }
                createUpchain(id)
                val repository = createRepository(id)
                val newItem = KeyValue(
                    key = id,
                    value = repository
                ).toItem()
                currentList + newItem
            }
        }

        private val accessUpchainMutex = Mutex()

        private suspend fun update(
            update: suspend (List<UpchainsRepository.Item>) -> List<UpchainsRepository.Item>,
        ) {
            accessUpchainMutex.withLock {
                while (true) {
                    val prevValue = upchains.value
                    val nextValue = update(prevValue)
                    if (upchains.compareAndSet(prevValue, nextValue)) {
                        return@withLock
                    }
                }
            }
        }


        private fun KeyValue<UpchainId, UpchainRepository>.toItem(): UpchainsRepository.Item =
            UpchainsRepository.Item(
                id = key,
                repository = value,
                remove = {
                    update { currentList ->
                        removeUpchain(key)
                        currentList.filter { it.id != key }
                    }
                },
            )
    }
}

