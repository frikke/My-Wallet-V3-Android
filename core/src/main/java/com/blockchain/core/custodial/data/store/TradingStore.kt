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
import com.blockchain.store.mapListData
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer
import piuk.blockchain.androidcore.utils.extensions.mapList

internal class TradingStore(
    private val balanceService: CustodialBalanceService,
    private val authenticator: Authenticator,
) : Store<List<TradingBalanceStoreModel>> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofSingle(
            mapper = {
                authenticator.authenticate {
                    balanceService.getTradingBalanceForAllAssets(it.authHeader)
                        .mapList { it.toStore() }
                }
            }
        ),
        dataSerializer = ListSerializer(TradingBalanceStoreModel.serializer()),
        mediator = FreshnessMediator(Freshness.DURATION_1_HOUR)
    ),
    TradingDataSource {

    override fun streamData(request: StoreRequest): Flow<StoreResponse<List<TradingBalance>>> =
        stream(request).mapListData { it.toDomain() }

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "TradingStore"
    }
}
