package com.blockchain.store.impl

import com.blockchain.store.Cache
import com.blockchain.store.CachedData
import com.blockchain.store.Parser
import com.blockchain.store.Persister
import com.blockchain.store.PersisterData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map

class PersistedCache<K, T>(
    private val persister: Persister,
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
}