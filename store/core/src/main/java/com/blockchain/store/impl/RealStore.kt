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

    private var previousEmissions: MutableList<Pair<Long, CachedData<K, T>?>> = mutableListOf()

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

        cache.read(request.key).distinctUntilChanged().collect { cachedData ->
            previousEmissions += System.currentTimeMillis() to cachedData

            val isStale = isStale(cachedData)
            val shouldFetch = networkLock.isActive && (request.forceRefresh || isStale)
            // We should only emit non stale data, however we put in this (networkLock.isCompleted || ...) in as a
            // safeguard, if we've already fetched we ignore the staleness and emit either way, just in case the fetch
            // result emission is considered stale by the mediator
            val shouldEmit = cachedData != null && (networkLock.isCompleted || !isStale)

            if (StoreConfig.DEBUG) {
                val wasMarkedAsStale = networkLock.isCompleted && cachedData?.lastFetched == 0L
                if (networkLock.isCompleted && isStale && !wasMarkedAsStale)
                    throw StoreException(
                        """New data was emitted from the cache that is stale after network had already completed, this 
                            |shouldn't happen, if it does it generally means that we've just fetched from the network 
                            |and the response was considered to be stale by the mediator.
                            |Currently stale cached data is not being emitted downstream.
                            |PS: Remember that after a FetchResult.Success the data is emitted via the cache as it is
                            |saved, it is not emitted directly from the RealStore.kt, so if you're seeing this crash it 
                            |most likely means that the data from FetchResult is considered stale by the mediator.
                            |networkLock.isCompleted: ${networkLock.isCompleted}, isStale: $isStale, 
                            |wasMarkedAsStale: $wasMarkedAsStale, cachedData: $cachedData, 
                            |currentTime: ${System.currentTimeMillis()}, previousEmissions: $previousEmissions
                        """.trimMargin()
                    )
            }
            if (shouldEmit) send(StoreResponse.Data(cachedData!!.data))

            if (networkLock.isActive) {
                when (shouldFetch) {
                    true -> networkLock.complete(Unit)
                    false -> networkLock.cancel()
                }
            }
        }
    }

    private fun buildFreshFlow(request: KeyedStoreRequest.Fresh<K>) = channelFlow {
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
            .distinctUntilChanged()
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

    override fun markStoreAsStale() {
        scope.launch {
            cache.markStoreAsStale()
        }
    }

    private fun isStale(cachedData: CachedData<K, T>?) = mediator.shouldFetch(cachedData)

    private class StoreException(override val message: String) : Exception()
}