package com.blockchain.store_caches_persistedjsonsqldelight

import store.StorePersisterDataQueries

class PersistedJsonSqlDelightCacheWiper(
    private val storePersisterDataQueries: StorePersisterDataQueries
) {

    suspend fun wipe() = storePersisterDataQueries.deleteAll()
}
