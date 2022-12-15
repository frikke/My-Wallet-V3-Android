package com.blockchain.core.buy.data.dataresources

import com.blockchain.nabu.models.responses.simplebuy.BuyOrderListResponse
import com.blockchain.nabu.models.responses.simplebuy.BuySellOrderResponse
import com.blockchain.nabu.service.NabuService
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.KeyedFlushableDataSource
import com.blockchain.utils.awaitOutcome
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

class BuyOrdersStore(
    private val nabuService: NabuService
) : KeyedStore<BuyOrdersStore.Key, BuyOrderListResponse> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofOutcome(
            mapper = {
                nabuService.getOutstandingOrders(
                    pendingOnly = it.pendingOnly
                ).awaitOutcome()
            }
        ),
        keySerializer = Key.serializer(),
        dataSerializer = ListSerializer(BuySellOrderResponse.serializer()),
        mediator = FreshnessMediator(Freshness.DURATION_1_HOUR)
    ),
    KeyedFlushableDataSource<BuyOrdersStore.Key> {

    override fun invalidate(param: Key) {
        markAsStale(param)
    }

    override fun invalidate() {
        markStoreAsStale()
    }

    @Serializable
    data class Key(
        val pendingOnly: Boolean
    )

    companion object {
        private const val STORE_ID = "BuyOrdersStore"
    }
}
