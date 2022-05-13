package com.blockchain.core.price.impl.assetpricestore

import com.blockchain.api.services.AssetPrice
import com.blockchain.api.services.AssetPriceService
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.impl.getStartTimeForTimeSpan
import com.blockchain.core.price.impl.suggestTimescaleInterval
import com.blockchain.core.price.model.AssetPriceError
import com.blockchain.core.price.model.AssetPriceRecord2
import com.blockchain.domain.common.model.toMillis
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.StoreRequest
import com.blockchain.store.firstOutcome
import com.blockchain.store_caches_inmemory.InMemoryCacheStoreBuilder
import info.blockchain.balance.Currency
import java.util.Calendar
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

internal class AssetPriceStoreCache(
    private val assetPriceService: AssetPriceService,
    private val supportedTickersStore: SupportedTickersStore
) : KeyedStore<
    AssetPriceStoreCache.Key,
    AssetPriceError,
    List<AssetPriceRecord2>
    > by InMemoryCacheStoreBuilder().buildKeyed(
    storeId = STORE_ID,
    fetcher = Fetcher.Keyed.ofOutcome { key ->
        supportedTickersStore
            .stream(StoreRequest.Cached(false))
            .firstOutcome()
            .flatMap { supportedTickers ->
                val single = when (key) {
                    is Key.GetAllCurrent -> assetPriceService.getCurrentPrices(
                        baseTickerList = supportedTickers.baseTickers.toSet(),
                        quoteTickerList = setOf(key.quoteTicker)
                    )
                    is Key.GetAllYesterday -> assetPriceService.getHistoricPrices(
                        baseTickers = supportedTickers.baseTickers.toSet(),
                        quoteTickers = setOf(key.quoteTicker),
                        time = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -1)
                        }.timeInMillis / 1000
                    )
                    is Key.GetHistorical -> assetPriceService.getHistoricPriceSeriesSince(
                        base = key.base.networkTicker,
                        quote = key.quoteTicker,
                        start = Calendar.getInstance().getStartTimeForTimeSpan(key.timeSpan, key.base),
                        scale = key.timeSpan.suggestTimescaleInterval()
                    )
                }

                single.awaitOutcome { error ->
                    AssetPriceError.RequestFailed(error.localizedMessage)
                }
            }.map {
                it.map { it.toAssetPriceRecord() }
            }
    },
    mediator = AssetPriceStoreMediator
) {

    sealed class Key {
        data class GetAllCurrent(val quoteTicker: String) : Key()
        data class GetAllYesterday(val quoteTicker: String) : Key()
        data class GetHistorical(
            val base: Currency,
            val quoteTicker: String,
            val timeSpan: HistoricalTimeSpan
        ) : Key()
    }

    companion object {
        private const val STORE_ID = "AssetPriceStoreCache"

        private fun AssetPrice.toAssetPriceRecord(): AssetPriceRecord2 =
            AssetPriceRecord2(
                base = this.base,
                quote = this.quote,
                rate = if (this.price.isNaN()) null else this.price.toBigDecimal(),
                fetchedAt = this.timestampSeconds.toMillis(),
                marketCap = this.marketCap,
            )
    }
}
