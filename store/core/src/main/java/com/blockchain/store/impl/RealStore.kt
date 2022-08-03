package com.blockchain.store.impl

import com.blockchain.data.DataResource
import com.blockchain.data.KeyedFreshnessStrategy
import com.blockchain.store.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class RealStore<K : Any,  T : Any>(
    private val scope: CoroutineScope,
    private val fetcher: Fetcher<K, T>,
    private val cache: Cache<K, T>,
    private val mediator: Mediator<K, T>
) : KeyedStore<K, T> {
    override fun stream(request: KeyedFreshnessStrategy<K>): Flow<DataResource<T>> =
        when (request) {
            is KeyedFreshnessStrategy.Cached -> buildCachedFlow(request)
            is KeyedFreshnessStrategy.Fresh -> buildFreshFlow(request)
        }.distinctUntilChanged()

    private var previousEmissions: MutableList<Pair<Long, CachedData<K, T>?>> = mutableListOf()

    private fun buildCachedFlow(request: KeyedFreshnessStrategy.Cached<K>) = channelFlow {

        val networkLock = CompletableDeferred<Unit>()
        scope.launch {
            networkLock.await()
            send(DataResource.Loading)
            when (val result = fetcher.fetch(request.key)) {
                is FetcherResult.Success -> {
                    // we're relying on the cache to emit the new value
                    cache.write(CachedData(request.key, result.value, System.currentTimeMillis()))
                }
                is FetcherResult.Failure -> send(DataResource.Error(result.error))
            }
        }

        cache.read(request.key).distinctUntilChanged().collectIndexed { index, cachedData ->
            previousEmissions += System.currentTimeMillis() to cachedData
            val isFirstEmission = index == 0

            // We only want to filter out stale emissions on the first cache emission, which contains the cached value,
            // the following emissions are always caused by `cache.write` which is only called on successful network
            // calls, in these cases we don't want to check for staleness and always emit.
            // This is done so even if the mediator considers a network response stale we don't skip that emission
            val isStale = isFirstEmission && isStale(cachedData)
            val shouldFetch = isFirstEmission && (request.forceRefresh || isStale)
            val shouldEmit = cachedData != null && !isStale

            if (shouldEmit) send(DataResource.Data(cachedData!!.data))

            if (isFirstEmission) {
                when (shouldFetch) {
                    true -> networkLock.complete(Unit)
                    false -> networkLock.cancel()
                }
            }
        }
    }

    private fun buildFreshFlow(request: KeyedFreshnessStrategy.Fresh<K>) = channelFlow {
        send(DataResource.Loading)
        val result = fetcher.fetch(request.key)
        when (result) {
            is FetcherResult.Success -> {
                cache.write(CachedData(request.key, result.value, System.currentTimeMillis()))
                send(DataResource.Data(result.value))
            }
            is FetcherResult.Failure -> send(DataResource.Error(result.error))
        }

        cache.read(request.key)
            .distinctUntilChanged()
            .filterNotNull()
            // we're relying on the cache to emit the new value and we're dropping it so we don't emit
            // duplicated Data events and we don't emit the currently cached value when it's an Error
            .drop(1)
            .map { DataResource.Data(it.data) }
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
}