package com.blockchain.store.impl

import com.blockchain.store.Fetcher
import com.blockchain.store.FetcherResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MulticasterFetcher<K, T, E>(
    private val fetcher: Fetcher<K, T, E>,
    private val scope: CoroutineScope
): Fetcher<K, T, E> {

    private val mutex = Mutex()
    private val currentCalls: MutableMap<K, Deferred<FetcherResult<T, E>>> = mutableMapOf()

    override suspend fun fetch(key: K): FetcherResult<T, E> {
        val call = mutex.withLock {
            val currentCall = currentCalls[key]
            if (currentCall == null || !currentCall.isActive) {
                val call = scope.async {
                    fetcher.fetch(key)
                }
                currentCalls[key] = call
                call
            } else {
                currentCall
            }
        }
        return call.await()
    }
}