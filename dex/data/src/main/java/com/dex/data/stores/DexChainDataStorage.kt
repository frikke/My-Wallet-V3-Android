package com.dex.data.stores

import com.blockchain.api.dex.DexApiService
import com.blockchain.api.dex.DexChainResponse
import com.blockchain.api.dex.DexTokenResponse
import com.blockchain.api.dex.DexTokensRequest
import com.blockchain.api.dex.DexVenueResponse
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import kotlinx.serialization.builtins.ListSerializer

class DexChainDataStorage(private val dexApiService: DexApiService) :
    Store<List<DexChainResponse>> by PersistedJsonSqlDelightStoreBuilder()
        .build(
            storeId = "DexChainDataStorage",
            fetcher = Fetcher.ofOutcome {
                dexApiService.dexChains()
            },
            dataSerializer = ListSerializer(DexChainResponse.serializer()),
            mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
        )

class DexTokensDataStorage(private val dexApiService: DexApiService) :
    KeyedStore<DexTokensRequest, List<DexTokenResponse>> by PersistedJsonSqlDelightStoreBuilder()
        .buildKeyed(
            storeId = "DexTokensDataStorage",
            fetcher = Fetcher.Keyed.ofOutcome(
                mapper = { key ->
                    dexApiService.dexTokens(key)
                }
            ),
            keySerializer = DexTokensRequest.serializer(),
            dataSerializer = ListSerializer(DexTokenResponse.serializer()),
            mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
        )

class DexVenuesDataStorage(private val dexApiService: DexApiService) :
    Store<List<DexVenueResponse>> by PersistedJsonSqlDelightStoreBuilder()
        .build(
            storeId = "DexVenuesDataStorage",
            fetcher = Fetcher.ofOutcome(
                mapper = {
                    dexApiService.dexVenues()
                }
            ),
            dataSerializer = ListSerializer(DexVenueResponse.serializer()),
            mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
        )
