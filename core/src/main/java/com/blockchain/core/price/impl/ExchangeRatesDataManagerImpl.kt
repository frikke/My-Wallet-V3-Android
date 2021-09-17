package com.blockchain.core.price.impl

import com.blockchain.api.services.AssetPriceService
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.Calendar

internal class ExchangeRatesDataManagerImpl(
    private val priceStore: AssetPriceStore,
    private val sparklineCall: SparklineCallCache,
    private val assetPriceService: AssetPriceService,
    private val currencyPrefs: CurrencyPrefs
) : ExchangeRatesDataManager {

    private val userFiat: String
        get() = currencyPrefs.selectedFiatCurrency

    override fun init(): Single<SupportedFiatTickerList> =
        priceStore.init()

    override fun cryptoToUserFiatRate(fromAsset: AssetInfo): Observable<ExchangeRate> =
        priceStore.getPriceForAsset(fromAsset.networkTicker, userFiat)
            .map {
                ExchangeRate.CryptoToFiat(
                    from = fromAsset,
                    to = it.quote,
                    rate = it.currentRate
                )
            }

    override fun fiatToUserFiatRate(fromFiat: String): Observable<ExchangeRate> =
        priceStore.getPriceForAsset(fromFiat, userFiat)
            .map {
                ExchangeRate.FiatToFiat(
                    from = fromFiat,
                    to = it.quote,
                    rate = it.currentRate
                )
            }

    override fun fiatToRateFiatRate(fromFiat: String, toFiat: String): Observable<ExchangeRate> =
        priceStore.getPriceForAsset(fromFiat, toFiat)
            .map {
                ExchangeRate.FiatToFiat(
                    from = fromFiat,
                    to = it.quote,
                    rate = it.currentRate
                )
            }

    override fun getLastCryptoToUserFiatRate(sourceCrypto: AssetInfo): ExchangeRate.CryptoToFiat {
        val price = priceStore.getCachedAssetPrice(sourceCrypto, userFiat)
        return ExchangeRate.CryptoToFiat(
            from = sourceCrypto,
            to = price.quote,
            rate = price.currentRate
        )
    }

    override fun getLastCryptoToFiatRate(
        sourceCrypto: AssetInfo,
        targetFiat: String
    ): ExchangeRate.CryptoToFiat {
        return when (targetFiat) {
            userFiat -> getLastCryptoToUserFiatRate(sourceCrypto)
            else -> getCryptoToFiatRate(sourceCrypto, targetFiat)
        }
    }

    override fun getLastFiatToCryptoRate(
        sourceFiat: String,
        targetCrypto: AssetInfo
    ): ExchangeRate.FiatToCrypto {
        return when (sourceFiat) {
            userFiat -> getLastCryptoToUserFiatRate(targetCrypto).inverse()
            else -> getCryptoToFiatRate(targetCrypto, sourceFiat).inverse()
        }
    }

    private fun getCryptoToFiatRate(
        sourceCrypto: AssetInfo,
        targetFiat: String
    ): ExchangeRate.CryptoToFiat {
        // Target fiat should always be one of userFiat or in the "api" fiat list, so we should
        // always have it. TODO: Add some checking for this case
        val price = priceStore.getCachedAssetPrice(sourceCrypto, targetFiat)
        return ExchangeRate.CryptoToFiat(
            from = sourceCrypto,
            to = price.quote,
            rate = price.currentRate
        )
    }

    override fun getLastFiatToUserFiatRate(sourceFiat: String): ExchangeRate.FiatToFiat {
        val price = priceStore.getCachedFiatPrice(sourceFiat, userFiat)
        return ExchangeRate.FiatToFiat(
            from = sourceFiat,
            to = price.quote,
            rate = price.currentRate
        )
    }

    override fun getLastFiatToFiatRate(sourceFiat: String, targetFiat: String): ExchangeRate.FiatToFiat {
        return when {
            sourceFiat == targetFiat -> ExchangeRate.FiatToFiat(
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
        fromAsset: AssetInfo,
        secSinceEpoch: Long
    ): Single<ExchangeRate> {
        return assetPriceService.getHistoricPrices(
            baseTickers = setOf(fromAsset.networkTicker),
            quoteTickers = setOf(userFiat),
            time = secSinceEpoch
        ).map { prices ->
            prices.first().let {
                ExchangeRate.CryptoToFiat(
                    from = fromAsset,
                    to = it.quote,
                    rate = it.price.toBigDecimal()
                )
            }
        }
    }

    override fun getPricesWith24hDelta(fromAsset: AssetInfo): Observable<Prices24HrWithDelta> =
        priceStore.getPriceForAsset(
            fromAsset.networkTicker,
            userFiat
        ).map { price ->
            Prices24HrWithDelta(
                delta24h = price.priceDelta(),
                previousRate = ExchangeRate.CryptoToFiat(
                    from = fromAsset,
                    to = price.quote,
                    rate = price.yesterdayRate
                ),
                currentRate = ExchangeRate.CryptoToFiat(
                    from = fromAsset,
                    to = price.quote,
                    rate = price.currentRate
                )
            )
        }

    override fun getHistoricPriceSeries(
        asset: AssetInfo,
        span: HistoricalTimeSpan,
        now: Calendar
    ): Single<HistoricalRateList> {
        require(asset.startDate != null)

        val scale = span.suggestTimescaleInterval()
        val startTime = now.getStartTimeForTimeSpan(span, asset)

        return assetPriceService.getHistoricPriceSeriesSince(
            base = asset.networkTicker,
            quote = userFiat,
            start = startTime,
            scale = scale
        ).toHistoricalRateList()
    }

    override fun get24hPriceSeries(
        asset: AssetInfo
    ): Single<HistoricalRateList> =
        sparklineCall.fetch(asset, userFiat)

    override val fiatAvailableForRates: List<String>
        get() = priceStore.fiatQuoteTickers
}
