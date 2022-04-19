package com.blockchain.store_caches_persistedjsonsqldelight

import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.KeyedStoreRequest
import com.blockchain.store.Mediator
import com.blockchain.store.Store
import com.blockchain.store.StoreId
import com.blockchain.store.StoreRequest
import com.blockchain.store.StoreResponse
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
    fun <E : Any, T : Any> build(
        storeId: StoreId,
        fetcher: Fetcher<Unit, E, T>,
        dataSerializer: KSerializer<T>,
        mediator: Mediator<Unit, T>,
        scope: CoroutineScope = GlobalScope
    ): Store<E, T> = object : Store<E, T> {
        private val backingStore = buildKeyed<Unit, E, T>(
            storeId = storeId,
            fetcher = fetcher,
            keySerializer = Unit.serializer(),
            dataSerializer = dataSerializer,
            mediator = mediator,
            scope = scope,
        )

        override fun stream(request: StoreRequest): Flow<StoreResponse<E, T>> = backingStore.stream(
            when (request) {
                StoreRequest.Fresh -> KeyedStoreRequest.Fresh(Unit)
                is StoreRequest.Cached -> KeyedStoreRequest.Cached(Unit, request.forceRefresh)
            }
        )

        override fun markAsStale() = backingStore.markAsStale(Unit)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun <K : Any, E : Any, T : Any> buildKeyed(
        storeId: StoreId,
        fetcher: Fetcher<K, E, T>,
        keySerializer: KSerializer<K>,
        dataSerializer: KSerializer<T>,
        mediator: Mediator<K, T>,
        scope: CoroutineScope = GlobalScope
    ): KeyedStore<K, E, T> = RealStore<K, E, T>(
        scope,
        MulticasterFetcher(fetcher, scope),
        PersistedJsonSqlDelightCache.Builder<K, T>(
            storeId,
            JsonParser(get(), keySerializer),
            JsonParser(get(), dataSerializer)
        ).build(),
        mediator
    )
}
