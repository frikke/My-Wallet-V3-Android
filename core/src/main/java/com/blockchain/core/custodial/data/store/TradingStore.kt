package com.blockchain.core.custodial.data.store

import com.blockchain.api.custodial.data.TradingBalanceResponseDto
import com.blockchain.api.services.CustodialBalanceService
import com.blockchain.nabu.Authenticator
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

class TradingStore(
    private val balanceService: CustodialBalanceService,
    private val authenticator: Authenticator,
) : Store<Throwable, Map<String, TradingBalanceResponseDto>> by PersistedJsonSqlDelightStoreBuilder()
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
        dataSerializer = MapSerializer(
            keySerializer = String.serializer(),
            valueSerializer = TradingBalanceResponseDto.serializer()
        ),
        mediator = FreshnessMediator(Freshness.DURATION_1_HOUR)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "TradingStore"
    }
}
