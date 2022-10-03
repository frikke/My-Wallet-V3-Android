package com.blockchain.store

import kotlinx.coroutines.flow.Flow

interface Persister {
    fun read(key: String?): Flow<PersisterData?>
    suspend fun write(data: PersisterData)

    /**
     * mark store as stale for [key]
     */
    suspend fun markAsStale(key: String?)

    /**
     * mark the whole store as stale for all keys
     */
    suspend fun markStoreAsStale()
}

data class PersisterData(
    val key: String?,
    val data: String,
    val lastFetched: Millis
)
