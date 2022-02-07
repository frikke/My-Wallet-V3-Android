package com.blockchain.core.price.impl

import com.blockchain.api.services.AssetPriceService
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.Calendar

internal class ExchangeRatesDataManagerImpl(
    private val priceStore: AssetPriceStore,
    private val sparklineCall: SparklineCallCache,
    private val assetPriceService: AssetPriceService,
    private val assetCatalogue: AssetCatalogue,
    private val currencyPrefs: CurrencyPrefs
) : ExchangeRatesDataManager {

    private val userFiat: Currency
        get() = currencyPrefs.selectedFiatCurrency

    override fun init(): Single<SupportedTickerList> =
        priceStore.init()

    override fun exchangeRate(fromAsset: Currency, toAsset: Currency): Observable<ExchangeRate> {
        val shouldInverse = fromAsset.type == CurrencyType.FIAT && toAsset.type == CurrencyType.CRYPTO
        val base = if (shouldInverse) toAsset else fromAsset
        val quote = if (shouldInverse) fromAsset else toAsset
        return priceStore.getPriceForAsset(base.networkTicker, quote.networkTicker).map {
            ExchangeRate(
                from = base,
                to = quote,
                rate = it.currentRate
            )
        }.map {
            if (shouldInverse)
                it.inverse()
            else it
        }
    }

    override fun exchangeRateToUserFiat(fromAsset: Currency): Observable<ExchangeRate> =
        priceStore.getPriceForAsset(fromAsset.networkTicker, userFiat.networkTicker).map {
            ExchangeRate(
                from = fromAsset,
                to = userFiat,
                rate = it.currentRate
            )
        }

    override fun getLastCryptoToUserFiatRate(sourceCrypto: AssetInfo): ExchangeRate {
        val price = priceStore.getCachedAssetPrice(sourceCrypto, userFiat)
        return ExchangeRate(
            from = sourceCrypto,
            to = userFiat,
            rate = price.currentRate
        )
    }

    override fun getLastCryptoToFiatRate(
        sourceCrypto: AssetInfo,
        targetFiat: FiatCurrency
    ): ExchangeRate {
        return when (targetFiat) {
            userFiat -> getLastCryptoToUserFiatRate(sourceCrypto)
            else -> getCryptoToFiatRate(sourceCrypto, targetFiat)
        }
    }

    override fun getLastFiatToCryptoRate(
        sourceFiat: FiatCurrency,
        targetCrypto: AssetInfo
    ): ExchangeRate {
        return when (sourceFiat) {
            userFiat -> getLastCryptoToUserFiatRate(targetCrypto).inverse()
            else -> getCryptoToFiatRate(targetCrypto, sourceFiat).inverse()
        }
    }

    private fun getCryptoToFiatRate(
        sourceCrypto: AssetInfo,
        targetFiat: FiatCurrency
    ): ExchangeRate {

        val price = priceStore.getCachedAssetPrice(sourceCrypto, targetFiat)
        return ExchangeRate(
            from = sourceCrypto,
            to = targetFiat,
            rate = price.currentRate
        )
    }

    override fun getLastFiatToUserFiatRate(sourceFiat: FiatCurrency): ExchangeRate {
        return when (sourceFiat) {
            userFiat -> ExchangeRate(
                from = sourceFiat,
                to = userFiat,
                rate = 1.0.toBigDecimal()
            )
            else -> {
                val price = priceStore.getCachedFiatPrice(sourceFiat, userFiat)
                return ExchangeRate(
                    from = sourceFiat,
                    to = userFiat,
                    rate = price.currentRate
                )
            }
        }
    }

    override fun getLastFiatToFiatRate(sourceFiat: FiatCurrency, targetFiat: FiatCurrency): ExchangeRate {
        return when {
            sourceFiat == targetFiat -> ExchangeRate(
                from = sourceFiat,
                to = targetFiat,
                rate = 1.0.toBigDecimal()
            )
            targetFiat == userFiat -> getLastFiatToUserFiatRate(sourceFiat)
            sourceFiat == userFiat -> getLastFiatToUserFiatRate(targetFiat).inverse()
            else -> throw IllegalStateException("Unknown fiats $sourceFiat -> $targetFiat")
        }
    }

    override fun getHistoricRate(
        fromAsset: Currency,
        secSinceEpoch: Long
    ): Single<ExchangeRate> {
        return assetPriceService.getHistoricPrices(
            baseTickers = setOf(fromAsset.networkTicker),
            quoteTickers = setOf(userFiat.networkTicker),
            time = secSinceEpoch
        ).map { prices ->
            ExchangeRate(
                from = fromAsset,
                to = userFiat,
                rate = prices.first().price.toBigDecimal()
            )
        }
    }

    override fun getPricesWith24hDelta(fromAsset: Currency): Observable<Prices24HrWithDelta> =
        getPricesWith24hDelta(fromAsset, userFiat)

    override fun getPricesWith24hDelta(fromAsset: Currency, fiat: Currency): Observable<Prices24HrWithDelta> =
        priceStore.getPriceForAsset(
            fromAsset.networkTicker,
            fiat.networkTicker
        ).map { price ->
            Prices24HrWithDelta(
                delta24h = price.priceDelta(),
                previousRate = ExchangeRate(
                    from = fromAsset,
                    to = fiat,
                    rate = price.yesterdayRate!!
                ),
                currentRate = ExchangeRate(
                    from = fromAsset,
                    to = fiat,
                    rate = price.currentRate!!
                )
            )
        }

    override fun getHistoricPriceSeries(
        asset: Currency,
        span: HistoricalTimeSpan,
        now: Calendar
    ): Single<HistoricalRateList> {
        require(asset.startDate != null)

        val scale = span.suggestTimescaleInterval()
        val startTime = now.getStartTimeForTimeSpan(span, asset)

        return assetPriceService.getHistoricPriceSeriesSince(
            base = asset.networkTicker,
            quote = userFiat.networkTicker,
            start = startTime,
            scale = scale
        ).toHistoricalRateList()
    }

    override fun get24hPriceSeries(
        asset: Currency
    ): Single<HistoricalRateList> =
        sparklineCall.fetch(asset, userFiat)

    override val fiatAvailableForRates: List<FiatCurrency>
        get() = priceStore.fiatQuoteTickers.mapNotNull {
            assetCatalogue.fiatFromNetworkTicker(it)
        }
}
