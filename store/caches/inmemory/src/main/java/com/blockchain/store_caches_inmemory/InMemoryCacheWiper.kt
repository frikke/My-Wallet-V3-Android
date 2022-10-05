package com.blockchain.store_caches_inmemory

import com.blockchain.store.Wiper

class InMemoryCacheWiper(
    private val provider: InMemoryCacheProvider
) : Wiper {
    override suspend fun wipe() = provider.wipeAll()
}
