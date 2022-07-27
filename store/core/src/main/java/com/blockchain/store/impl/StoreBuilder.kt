package com.blockchain.store.impl

import com.blockchain.store.Cache
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.KeyedStoreRequest
import com.blockchain.store.Mediator
import com.blockchain.store.Store
import com.blockchain.store.StoreRequest
import com.blockchain.store.StoreResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow

class StoreBuilder {

    @OptIn(DelicateCoroutinesApi::class)
    fun <E : Any, T : Any> build(
        fetcher: Fetcher<Unit, T>,
        cache: Cache<Unit, T>,
        mediator: Mediator<Unit, T>,
        scope: CoroutineScope = GlobalScope
    ): Store<T> = object : Store<T> {
        private val backingStore = buildKeyed<Unit, E, T>(
            fetcher = fetcher,
            cache = cache,
            mediator = mediator,
            scope = scope,
        )

        override fun stream(request: StoreRequest): Flow<StoreResponse<T>> = backingStore.stream(
            when (request) {
                StoreRequest.Fresh -> KeyedStoreRequest.Fresh(Unit)
                is StoreRequest.Cached -> KeyedStoreRequest.Cached(Unit, request.forceRefresh)
            }
        )

        override fun markAsStale() = backingStore.markAsStale(Unit)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun <K : Any, E : Any, T : Any> buildKeyed(
        fetcher: Fetcher<K,  T>,
        cache: Cache<K, T>,
        mediator: Mediator<K, T>,
        scope: CoroutineScope = GlobalScope
    ): KeyedStore<K,  T> = RealStore<K, T>(
        scope,
        MulticasterFetcher(fetcher, scope),
        cache,
        mediator
    )
}