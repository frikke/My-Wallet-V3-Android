package com.blockchain.store_caches_persistedjsonsqldelight

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.KeyedFreshnessStrategy
import com.blockchain.store.CacheConfiguration
import com.blockchain.store.Fetcher
import com.blockchain.store.Mediator
import com.blockchain.store.Store
import com.blockchain.store.StoreId
import com.blockchain.store.impl.JsonParser
import com.blockchain.store.impl.MulticasterFetcher
import com.blockchain.store.impl.RealStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class PersistedJsonSqlDelightStoreBuilder : KoinComponent {

    @OptIn(DelicateCoroutinesApi::class)
    inline fun <reified T : Any> build(
        storeId: StoreId,
        reset: CacheConfiguration = CacheConfiguration.default(),
        fetcher: Fetcher<Unit, T>,
        dataSerializer: KSerializer<T>,
        mediator: Mediator<Unit, T>,
        scope: CoroutineScope = GlobalScope
    ): Store<T> = object : Store<T> {
        private val backingStore = buildKeyed(
            storeId = storeId,
            fetcher = fetcher,
            reset = reset,
            keySerializer = Unit.serializer(),
            dataSerializer = dataSerializer,
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
        fetcher: Fetcher<K, T>,
        reset: CacheConfiguration = CacheConfiguration.default(),
        keySerializer: KSerializer<K>,
        dataSerializer: KSerializer<T>,
        mediator: Mediator<K, T>,
        scope: CoroutineScope = GlobalScope
    ): RealStore<K, T> = RealStore(
        scope,
        MulticasterFetcher(fetcher, scope),
        PersistedJsonSqlDelightCache.Builder(
            storeId,
            JsonParser(get(), keySerializer),
            JsonParser(get(), dataSerializer)
        ).build(),
        reset = reset,
        mediator = mediator,
        notificationReceiver = get()
    )
}
