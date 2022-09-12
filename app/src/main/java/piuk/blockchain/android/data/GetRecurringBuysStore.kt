package piuk.blockchain.android.data

import com.blockchain.api.services.TradeService
import com.blockchain.api.trade.data.RecurringBuyResponse
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.KeyedFlushableDataSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

class GetRecurringBuysStore(
    private val tradeService: TradeService
) : KeyedStore<GetRecurringBuysStore.Key, List<RecurringBuyResponse>> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = { key ->
                tradeService.getRecurringBuysForAsset(
                    assetTicker = key.networkTicker
                )
            }
        ),
        keySerializer = Key.serializer(),
        dataSerializer = ListSerializer(RecurringBuyResponse.serializer()),
        mediator = FreshnessMediator(Freshness.ofHours(24))
    ),
    KeyedFlushableDataSource<GetRecurringBuysStore.Key> {

    @Serializable
    data class Key(
        val networkTicker: String
    )

    override fun invalidate(param: Key) {
        markAsStale(param)
    }

    override fun invalidate() {
        markStoreAsStale()
    }

    companion object {
        private const val STORE_ID = "GetRecurringBuysStore"
    }
}
