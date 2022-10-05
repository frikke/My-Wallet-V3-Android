package com.blockchain.store_persisters_persistedjsonsqldelight

import com.blockchain.store.PersisterData
import com.blockchain.store.StoreId
import com.blockchain.store_caches_persistedjsonsqldelight.SqlDelightStoreIdScopedPersister
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import store.StorePersisterData
import store.StorePersisterDataQueries

internal class SqlDelightStoreIdScopedPersisterImpl(
    private val storeId: StoreId,
    private val storePersisterDataQueries: StorePersisterDataQueries
) : SqlDelightStoreIdScopedPersister {
    override fun read(key: String?): Flow<PersisterData?> =
        storePersisterDataQueries.selectByStoreIdAndKey(storeId, key)
            .asFlow()
            .mapToOneOrNull()
            .map { data ->
                if (data == null) return@map null
                PersisterData(data.key, data.data_, data.last_fetched)
            }

    override suspend fun write(data: PersisterData) = storePersisterDataQueries.insert(
        StorePersisterData(storeId, data.key, data.data, data.lastFetched)
    )

    override suspend fun markAsStale(key: String?) = storePersisterDataQueries.markAsStale(storeId, key)

    override suspend fun markStoreAsStale() = storePersisterDataQueries.markStoreAsStale(storeId)
}
