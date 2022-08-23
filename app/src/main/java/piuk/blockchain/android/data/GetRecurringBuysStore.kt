package piuk.blockchain.android.data

import com.blockchain.api.services.TradeService
import com.blockchain.api.trade.data.RecurringBuyResponse
import com.blockchain.coincore.NullCryptoAddress.asset
import com.blockchain.nabu.Authenticator
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.serialization.builtins.ListSerializer

class GetRecurringBuysStore(
    private val authenticator: Authenticator,
    private val tradeService: TradeService
) : Store<List<RecurringBuyResponse>> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = {
                authenticator.authenticate { tokenResponse ->
                    tradeService.getRecurringBuysForAsset(authHeader = tokenResponse.authHeader, asset.networkTicker)
                }
            }
        ),
        dataSerializer = ListSerializer(RecurringBuyResponse.serializer()),
        mediator = FreshnessMediator(Freshness.ofHours(24))
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "GetRecurringBuysStore"
    }
}
