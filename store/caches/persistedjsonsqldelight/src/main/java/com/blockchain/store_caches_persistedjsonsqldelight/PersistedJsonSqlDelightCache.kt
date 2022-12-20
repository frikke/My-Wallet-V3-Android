package com.blockchain.store_caches_persistedjsonsqldelight

import com.blockchain.store.Cache
import com.blockchain.store.CachedData
import com.blockchain.store.Parser
import com.blockchain.store.PersisterData
import com.blockchain.store.StoreId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

class PersistedJsonSqlDelightCache<K, T> internal constructor(
    private val persister: SqlDelightStoreIdScopedPersister,
    private val keyParser: Parser<K>,
    private val dataParser: Parser<T>
) : Cache<K, T> {
    override fun read(key: K): Flow<CachedData<K, T>?> =
        persister.read(keyParser.encode(key)).map { persisterData ->
            if (persisterData == null) return@map null
            val parsedData = dataParser.decode(persisterData.data) ?: return@map null

            CachedData(key, parsedData, persisterData.lastFetched)
        }

    override suspend fun write(cachedData: CachedData<K, T>) =
        persister.write(
            PersisterData(keyParser.encode(cachedData.key), dataParser.encode(cachedData.data), cachedData.lastFetched)
        )

    override suspend fun markAsStale(key: K) = persister.markAsStale(keyParser.encode(key))

    override suspend fun markStoreAsStale() = persister.markStoreAsStale()

    class Builder<K, T>(
        private val storeId: StoreId,
        private val keyParser: Parser<K>,
        private val dataParser: Parser<T>
    ) : KoinComponent {
        fun build(): PersistedJsonSqlDelightCache<K, T> =
            PersistedJsonSqlDelightCache(
                get { parametersOf(storeId) },
                keyParser,
                dataParser
            )
    }
}
