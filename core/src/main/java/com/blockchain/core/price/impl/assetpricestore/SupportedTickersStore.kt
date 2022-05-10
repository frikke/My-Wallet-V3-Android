package com.blockchain.core.price.impl.assetpricestore

import com.blockchain.api.services.AssetPriceService
import com.blockchain.core.price.model.AssetPriceError
import com.blockchain.store.CachedData
import com.blockchain.store.Fetcher
import com.blockchain.store.Mediator
import com.blockchain.store.Store
import com.blockchain.store_caches_inmemory.InMemoryCacheStoreBuilder

internal class SupportedTickersStore(
    private val assetPriceService: AssetPriceService
) : Store<AssetPriceError, SupportedTickerGroup> by InMemoryCacheStoreBuilder().build(
    storeId = STORE_ID,
    fetcher = Fetcher.ofSingle(
        mapper = {
            assetPriceService.getSupportedCurrencies().map { symbols ->
                SupportedTickerGroup(
                    baseTickers = symbols.base.map { it.ticker },
                    fiatQuoteTickers = symbols.quote.filter { it.isFiat }.map { it.ticker }
                )
            }
        },
        errorMapper = { error ->
            AssetPriceError.RequestFailed(error.localizedMessage)
        }
    ),
    mediator = object : Mediator<Unit, SupportedTickerGroup> {
        override fun shouldFetch(cachedData: CachedData<Unit, SupportedTickerGroup>?): Boolean =
            cachedData == null
    }
) {
    companion object {
        private const val STORE_ID = "SupportedTickersStore"
    }
}
