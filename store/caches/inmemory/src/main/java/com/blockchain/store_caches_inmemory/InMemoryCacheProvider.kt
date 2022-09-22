package com.blockchain.store_caches_inmemory

import com.blockchain.store.StoreId
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object InMemoryCacheProvider {

    private val mutex = Mutex()

    private val caches: MutableMap<StoreId, InMemoryCache<*, *>> = mutableMapOf()

    suspend fun wipeAll() {
        mutex.withLock {
            caches.clear()
        }
    }

    fun <K, T> provide(storeId: StoreId): InMemoryCache<K, T> = runBlocking {
        mutex.withLock {
            val cache = caches[storeId]
            if (cache != null) {
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
