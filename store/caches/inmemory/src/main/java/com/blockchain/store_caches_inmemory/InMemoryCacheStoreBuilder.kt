package com.blockchain.store_caches_inmemory

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.KeyedFreshnessStrategy
import com.blockchain.store.CacheConfiguration
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.Mediator
import com.blockchain.store.Store
import com.blockchain.store.StoreId
import com.blockchain.store.impl.MulticasterFetcher
import com.blockchain.store.impl.RealStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class InMemoryCacheStoreBuilder : KoinComponent {
    @OptIn(DelicateCoroutinesApi::class)
    fun <T : Any> build(
        storeId: StoreId,
        reset: CacheConfiguration = CacheConfiguration.default(),
        fetcher: Fetcher<Unit, T>,
        mediator: Mediator<Unit, T>,
        scope: CoroutineScope = GlobalScope
    ): Store<T> = object : Store<T> {
        private val backingStore = buildKeyed(
            storeId = storeId,
            fetcher = fetcher,
            reset = reset,
            mediator = mediator,
            scope = scope
        )

        override fun stream(request: FreshnessStrategy): Flow<DataResource<T>> = backingStore.stream(
            when (request) {
                FreshnessStrategy.Fresh -> KeyedFreshnessStrategy.Fresh(Unit)
                is FreshnessStrategy.Cached -> KeyedFreshnessStrategy.Cached(Unit, request.refreshStrategy)
            }
        )

        override fun markAsStale() = backingStore.markAsStale(Unit)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun <K : Any, T : Any> buildKeyed(
        storeId: StoreId,
        reset: CacheConfiguration = CacheConfiguration.default(),
        fetcher: Fetcher<K, T>,
        mediator: Mediator<K, T>,
        scope: CoroutineScope = GlobalScope
    ): KeyedStore<K, T> = RealStore(
        scope,
        MulticasterFetcher(fetcher, scope),
        InMemoryCacheProvider.provide(storeId),
        mediator,
        reset,
        notificationReceiver = get()
    )
}
