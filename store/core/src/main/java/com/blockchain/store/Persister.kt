package com.blockchain.store

import kotlinx.coroutines.flow.Flow

interface Persister {
    fun read(key: String?): Flow<PersisterData?>
    suspend fun write(data: PersisterData)
    suspend fun markAsStale(key: String?)
}

data class PersisterData(
    val key: String?,
    val data: String,
    val lastFetched: Millis
)