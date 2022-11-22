package com.blockchain.core.chains.dynamicselfcustody.data

import com.blockchain.api.coinnetworks.data.CoinTypeDto
import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.serialization.builtins.ListSerializer

class CoinTypeStore(
    private val discoveryService: AssetDiscoveryApiService
) : Store<List<CoinTypeDto>> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofOutcome(
            mapper = {
                discoveryService.allCoinTypes()
            }
        ),
        dataSerializer = ListSerializer(CoinTypeDto.serializer()),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "CoinTypeStore"
    }
}
