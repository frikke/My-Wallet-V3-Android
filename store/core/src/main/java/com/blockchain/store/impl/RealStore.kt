package com.blockchain.store.impl

import com.blockchain.store.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class RealStore<K : Any, E : Any, T : Any>(
    private val scope: CoroutineScope,
    private val fetcher: Fetcher<K, E, T>,
    private val cache: Cache<K, T>,
    private val mediator: Mediator<K, T>
) : KeyedStore<K, E, T> {
    override fun stream(request: KeyedStoreRequest<K>): Flow<StoreResponse<E, T>> =
        when (request) {
            is KeyedStoreRequest.Cached -> buildCachedFlow(request)
            is KeyedStoreRequest.Fresh -> buildFreshFlow(request)
        }.distinctUntilChanged()

    private fun buildCachedFlow(request: KeyedStoreRequest.Cached<K>) = channelFlow<StoreResponse<E, T>> {
        val networkLock = CompletableDeferred<Unit>()
        scope.launch {
            networkLock.await()
            send(StoreResponse.Loading)
            when (val result = fetcher.fetch(request.key)) {
                is FetcherResult.Success -> {
                    // we're relying on the cache to emit the new value
                    cache.write(CachedData(request.key, result.value, System.currentTimeMillis()))
                }
                is FetcherResult.Failure -> send(StoreResponse.Error(result.error))
            }
        }

        cache.read(request.key).collect { cachedData ->
            if (cachedData != null) send(StoreResponse.Data(cachedData.data))
            if (networkLock.isActive) {
                when (shouldFetch(request, cachedData)) {
                    true -> networkLock.complete(Unit)
                    false -> networkLock.cancel()
                }
            }
        }
    }

    private fun buildFreshFlow(request: KeyedStoreRequest.Fresh<K>) = channelFlow<StoreResponse<E, T>> {
        send(StoreResponse.Loading)
        val result = fetcher.fetch(request.key)
        when (result) {
            is FetcherResult.Success -> {
                cache.write(CachedData(request.key, result.value, System.currentTimeMillis()))
                send(StoreResponse.Data(result.value))
            }
            is FetcherResult.Failure -> send(StoreResponse.Error(result.error))
        }

        cache.read(request.key)
            .filterNotNull()
            // we're relying on the cache to emit the new value and we're dropping it so we don't emit
            // duplicated Data events and we don't emit the currently cached value when it's an Error
            .drop(1)
            .map { StoreResponse.Data(it.data) }
            .collect { send(it) }
    }

    override fun markAsStale(key: K) {
        scope.launch {
            cache.markAsStale(key)
        }
    }

    private fun shouldFetch(request: KeyedStoreRequest.Cached<K>, cachedData: CachedData<K, T>?): Boolean =
        request.forceRefresh || mediator.shouldFetch(cachedData)
}