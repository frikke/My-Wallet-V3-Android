package com.blockchain.coincore.loader

import com.blockchain.api.selfcustody.BalancesResponse
import com.blockchain.api.services.DynamicSelfCustodyService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.IsCachedMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource

internal class ActiveBalancesStore(
    private val currencyPrefs: CurrencyPrefs,
    private val selfCustodyService: DynamicSelfCustodyService
) : Store<BalancesResponse> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofOutcome(
            mapper = {
                selfCustodyService.getBalances(
                    fiatCurrency = currencyPrefs.selectedFiatCurrency.networkTicker
                )
            }
        ),
        dataSerializer = BalancesResponse.serializer(),
        mediator = IsCachedMediator()
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "ActiveBalancesStore"
    }
}
