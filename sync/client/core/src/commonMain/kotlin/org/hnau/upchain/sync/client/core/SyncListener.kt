package org.hnau.upchain.sync.client.core

data class SyncListener(
    val onUpdatesFromServers: (updatesCount: Int) -> Unit = {},
    val onApplyServerUpdates: (updatesCount: Int) -> Unit = {},
    val onUpdatesToServer: (updatesCount: Int) -> Unit = {},
)