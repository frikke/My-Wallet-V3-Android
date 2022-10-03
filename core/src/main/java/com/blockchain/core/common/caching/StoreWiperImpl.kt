package com.blockchain.core.common.caching

import com.blockchain.store_caches_inmemory.InMemoryCacheWiper
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightCacheWiper
import com.blockchain.storedatasource.StoreWiper

class StoreWiperImpl(
    private val inMemoryCacheWiper: InMemoryCacheWiper,
    private val persistedJsonSqlDelightCacheWiper: PersistedJsonSqlDelightCacheWiper,
) : StoreWiper {

    override suspend fun wipe() {
        inMemoryCacheWiper.wipe()
        persistedJsonSqlDelightCacheWiper.wipe()
    }
}
