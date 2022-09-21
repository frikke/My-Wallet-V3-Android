package com.blockchain.store_caches_inmemory

class InMemoryCacheWiper(
    private val provider: InMemoryCacheProvider
) {
    suspend fun wipe() = provider.wipeAll()
}
