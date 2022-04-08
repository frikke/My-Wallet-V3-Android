package com.blockchain.store.impl

import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.KeyedStoreRequest
import com.blockchain.store.Persister
import com.blockchain.store.Store
import com.blockchain.store.StoreId
import com.blockchain.store.StoreRequest
import com.blockchain.store.StoreResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class JsonPersistedStoreBuilder : KoinComponent {

    @OptIn(DelicateCoroutinesApi::class)
    fun <E : Any, T : Any> build(
        storeId: StoreId,
        fetcher: Fetcher<Unit, E, T>,
        persister: Persister,
        dataSerializer: KSerializer<T>,
        freshness: Freshness,
        scope: CoroutineScope = GlobalScope
    ): Store<E, T> = object : Store<E, T> {
        private val backingStore = buildKeyed<Unit, E, T>(
            storeId = storeId,
            fetcher = fetcher,
            persister = persister,
            keySerializer = Unit.serializer(),
            dataSerializer = dataSerializer,
            freshness = freshness,
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
        persister: Persister,
        keySerializer: KSerializer<K>,
        dataSerializer: KSerializer<T>,
        freshness: Freshness,
        scope: CoroutineScope = GlobalScope
    ): KeyedStore<K, E, T> = RealStore<K, E, T>(
        scope,
        MulticasterFetcher(fetcher, scope),
        PersistedCache(
            persister,
            JsonParser(
                json = get(),
                serializer = keySerializer
            ),
            JsonParser(
                json = get(),
                serializer = dataSerializer
            )
        ),
        FreshnessMediator(freshness)
    )
}