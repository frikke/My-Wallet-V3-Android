package com.blockchain.core.price.impl.assetpricestore

import com.blockchain.api.services.AssetPriceService
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder

internal class SupportedTickersStore(
    private val assetPriceService: AssetPriceService
) : Store<SupportedTickerGroup> by PersistedJsonSqlDelightStoreBuilder().build(
    storeId = STORE_ID,
    fetcher = Fetcher.ofSingle(
        mapper = {
            assetPriceService.getSupportedCurrencies().map { symbols ->
                SupportedTickerGroup(
                    baseTickers = symbols.base.map { it.ticker },
                    fiatQuoteTickers = symbols.quote.filter { it.isFiat }.map { it.ticker }
                )
            }
        }
    ),
    mediator = FreshnessMediator(Freshness.ofHours(3 * 24)),
    dataSerializer = SupportedTickerGroup.serializer()
) {
    companion object {
        private const val STORE_ID = "SupportedTickersStore"
    }
}
