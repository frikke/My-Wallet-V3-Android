package com.blockchain.core.custodial.data.store

import com.blockchain.api.services.CustodialBalanceService
import com.blockchain.api.services.TradingBalance
import com.blockchain.nabu.Authenticator
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.StoreRequest
import com.blockchain.store.StoreResponse
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer

internal class TradingStore(
    private val balanceService: CustodialBalanceService,
    private val authenticator: Authenticator
) : Store<Throwable, List<TradingBalance>> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofSingle(
            mapper = {
                authenticator.authenticate {
                    balanceService.getTradingBalanceForAllAssets(it.authHeader)
                }
            },
            errorMapper = { it }
        ),
        dataSerializer = ListSerializer(TradingBalance.serializer()),
        mediator = FreshnessMediator(Freshness.ofMinutes(60L)) // todo(othman) duration?
    ),
    TradingDataSource {

    override fun stream(refresh: Boolean): Flow<StoreResponse<Throwable, List<TradingBalance>>> =
        stream(StoreRequest.Cached(refresh))

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "TradingStore"
    }
}
