package com.blockchain.store.impl

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.KeyedFreshnessStrategy
import com.blockchain.store.Cache
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.Mediator
import com.blockchain.store.Store
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

        override fun stream(request: FreshnessStrategy): Flow<DataResource<T>> = backingStore.stream(
            when (request) {
                FreshnessStrategy.Fresh -> KeyedFreshnessStrategy.Fresh(Unit)
                is FreshnessStrategy.Cached -> KeyedFreshnessStrategy.Cached(Unit, request.forceRefresh)
            }
        )

        override fun markAsStale() = backingStore.markAsStale(Unit)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun <K : Any, E : Any, T : Any> buildKeyed(
        fetcher: Fetcher<K, T>,
        cache: Cache<K, T>,
        mediator: Mediator<K, T>,
        scope: CoroutineScope = GlobalScope
    ): KeyedStore<K, T> = RealStore(
        scope,
        MulticasterFetcher(fetcher, scope),
        cache,
        mediator
    )
}
