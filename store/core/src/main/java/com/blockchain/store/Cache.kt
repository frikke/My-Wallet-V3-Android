package com.blockchain.store

import kotlinx.coroutines.flow.Flow

interface Cache<K, T> {
    fun read(key: K): Flow<CachedData<K, T>?>
    suspend fun write(cachedData: CachedData<K, T>)
    suspend fun markAsStale(key: K)
    suspend fun markStoreAsStale()
}

data class CachedData<K, T>(val key: K, val data: T, val lastFetched: Millis)