package com.blockchain.core.price.impl.assetpricestore

import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.model.AssetPriceNotFoundException
import com.blockchain.core.price.model.AssetPriceRecord
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.firstOutcome
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.map
import info.blockchain.balance.Currency
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal typealias SupportedTickerList = List<String>

internal class AssetPriceStore(
    private val cache: AssetPriceStoreCache,
    private val supportedTickersStore: SupportedTickersStore
) {

    private val quoteTickerToCurrentPrices = ConcurrentHashMap<String, List<AssetPriceRecord>>()
    lateinit var fiatQuoteTickers: SupportedTickerList
        private set

    internal suspend fun warmSupportedTickersCache(): Outcome<Exception, Unit> =
        supportedTickersStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            .firstOutcome()
            .doOnSuccess { tickerGroup ->
                fiatQuoteTickers = tickerGroup.fiatQuoteTickers
            }
            .map {
                @Suppress("RedundantUnitExpression")
                Unit
            }

    internal fun getCurrentPriceForAsset(
        base: Currency,
        quote: Currency
    ): Flow<DataResource<AssetPriceRecord>> =
        if (base.networkTicker == quote.networkTicker) {
            flowOf(createEqualityRecordResponse(base.networkTicker, quote.networkTicker))
        } else {
            cache.stream(
                FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale).withKey(
                    AssetPriceStoreCache.Key.GetAllCurrent(quote.networkTicker)
                )
            ).onEach { response ->
                if (response is DataResource.Data) {
                    quoteTickerToCurrentPrices.putAll(response.data.groupBy { it.quote })
                }
            }.findAssetOrError(base, quote)
                .distinctUntilChanged()
        }

    internal fun getYesterdayPriceForAsset(
        base: Currency,
        quote: Currency
    ): Flow<DataResource<AssetPriceRecord>> =
        if (base.networkTicker == quote.networkTicker) {
            flowOf(createEqualityRecordResponse(base.networkTicker, quote.networkTicker))
        } else {
            cache.stream(
                FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                    .withKey(
                        AssetPriceStoreCache.Key.GetAllYesterday(quote.networkTicker)
                    )
            ).findAssetOrError(base, quote)
                .distinctUntilChanged()
        }

    internal fun getHistoricalPriceForAsset(
        base: Currency,
        quote: Currency,
        timeSpan: HistoricalTimeSpan
    ): Flow<DataResource<List<AssetPriceRecord>>> = cache.stream(
        FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale).withKey(
            AssetPriceStoreCache.Key.GetHistorical(base, quote.networkTicker, timeSpan)
        )
    )

    fun getCachedAssetPrice(fromAsset: Currency, toFiat: Currency): AssetPriceRecord =
        quoteTickerToCurrentPrices[toFiat.networkTicker]
            ?.find { it.base == fromAsset.networkTicker }
            ?: throw AssetPriceNotFoundException(fromAsset.networkTicker, toFiat.networkTicker)

    fun getCachedFiatPrice(fromFiat: Currency, toFiat: Currency): AssetPriceRecord =
        quoteTickerToCurrentPrices[toFiat.networkTicker]
            ?.find { it.base == fromFiat.networkTicker }
            ?: throw AssetPriceNotFoundException(fromFiat.networkTicker, toFiat.networkTicker)

    private fun Flow<DataResource<List<AssetPriceRecord>>>.findAssetOrError(
        base: Currency,
        quote: Currency
    ): Flow<DataResource<AssetPriceRecord>> =
        map { response ->
            when (response) {
                is DataResource.Data -> {
                    val assetPrice = response.data.find {
                        it.base == base.networkTicker && it.quote == quote.networkTicker
                    }
                    if (assetPrice != null) {
                        DataResource.Data(assetPrice)
                    } else DataResource.Error(AssetPriceNotFoundException(base, quote))
                }
                is DataResource.Error -> response
                is DataResource.Loading -> response
            }
        }

    private fun createEqualityRecordResponse(
        base: String,
        quote: String
    ): DataResource<AssetPriceRecord> = DataResource.Data(
        AssetPriceRecord(
            base = base,
            quote = quote,
            rate = 1.0.toBigDecimal(),
            fetchedAt = Calendar.getInstance().timeInMillis,
            marketCap = 0.0
        )
    )
}
