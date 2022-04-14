package com.blockchain.store_caches_inmemory

import com.blockchain.store.Cache
import com.blockchain.store.CachedData
import com.blockchain.store.StoreId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryCache<K, T> private constructor() : Cache<K, T> {

    private val mutex = Mutex()
    // Using SharedFlow rather than StateFlow because Store relies on [read] to always emit regardless of the value being distinct or not
    // So we're essentially creating a StateFlow without the distinct.
    private val cache = MutableSharedFlow<Map<K, CachedData<K, T>>>(replay = 1).apply {
        tryEmit(emptyMap())
    }

    override fun read(key: K): Flow<CachedData<K, T>?> = cache.map {
        it[key]
    }

    override suspend fun write(cachedData: CachedData<K, T>) {
        mutex.withLock {
            val oldCache = cache.replayCache.first()
            cache.emit(oldCache.plus(cachedData.key to cachedData))
        }
    }

    override suspend fun markAsStale(key: K) {
        mutex.withLock {
            val oldCache = cache.replayCache.first()
            val oldEntry = oldCache[key]

            if (oldEntry != null) {
                val newEntry = oldEntry.copy(lastFetched = 0L)
                cache.emit(oldCache.plus(key to newEntry))
            }
        }
    }

    class Builder(private val storeId: StoreId) {
        private val caches: MutableMap<StoreId, InMemoryCache<*, *>> = mutableMapOf()

        @Synchronized
        fun <K, T> build(): InMemoryCache<K, T> {
            val cache = caches[storeId]
            return if (cache != null) {
                @Suppress("UNCHECKED_CAST")
                cache as InMemoryCache<K, T>
            } else {
                InMemoryCache<K, T>().also {
                    caches[storeId] = it
                }
            }
        }
    }
}