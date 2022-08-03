package com.blockchain.store.impl

import com.blockchain.store.Fetcher
import com.blockchain.store.FetcherResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MulticasterFetcher<K, T>(
    private val fetcher: Fetcher<K, T>,
    private val scope: CoroutineScope
) : Fetcher<K, T> {

    private val mutex = Mutex()
    private val currentCalls: MutableMap<K, Deferred<FetcherResult<T>>> = mutableMapOf()

    override suspend fun fetch(key: K): FetcherResult<T> {
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
