package com.blockchain.store_persisters_persistedjsonsqldelight

import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightCacheWiper
import store.StorePersisterDataQueries

class PersistedJsonSqlDelightCacheWiperImpl(
    private val storePersisterDataQueries: StorePersisterDataQueries
) : PersistedJsonSqlDelightCacheWiper {
    override suspend fun wipe() = storePersisterDataQueries.deleteAll()
}
