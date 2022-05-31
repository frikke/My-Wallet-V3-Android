package com.blockchain.core.price.impl

import com.blockchain.api.services.AssetPriceService
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.HistoricalRate
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.core.price.impl.assetpricestore.AssetPriceStore2
import com.blockchain.core.price.model.AssetPriceError
import com.blockchain.core.price.model.AssetPriceNotCached
import com.blockchain.core.price.model.AssetPriceRecord2
import com.blockchain.domain.common.model.toSeconds
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.map
import com.blockchain.outcome.mapLeft
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.asObservable
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.RoundingMode
import java.util.Calendar
import piuk.blockchain.androidcore.utils.extensions.rxCompletableOutcome
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome

internal class ExchangeRatesDataManagerImpl(
    private val priceStore: AssetPriceStore,
    private val priceStore2: AssetPriceStore2,
    private val newAssetPriceStoreFeatureFlag: FeatureFlag,
    private val sparklineCall: SparklineCallCache,
    private val assetPriceService: AssetPriceService,
    private val assetCatalogue: AssetCatalogue,
    private val currencyPrefs: CurrencyPrefs
) : ExchangeRatesDataManager {

    private val userFiat: Currency
        get() = currencyPrefs.selectedFiatCurrency

    private var isNewAssetPriceStoreFFEnabledCached = false
    private fun isNewAssetPriceStoreFFEnabled() = newAssetPriceStoreFeatureFlag.enabled.doOnSuccess { enabled ->
        isNewAssetPriceStoreFFEnabledCached = enabled
    }

    override fun init(): Completable =
        isNewAssetPriceStoreFFEnabled().flatMapCompletable { enabled ->
            if (enabled) rxCompletableOutcome { priceStore2.warmSupportedTickersCache().mapLeft(::toRxThrowable) }
            else priceStore.init().ignoreElement()
        }

    override fun exchangeRate(fromAsset: Currency, toAsset: Currency): Observable<ExchangeRate> {
        val shouldInverse = fromAsset.type == CurrencyType.FIAT && toAsset.type == CurrencyType.CRYPTO
        val base = if (shouldInverse) toAsset else fromAsset
        val quote = if (shouldInverse) fromAsset else toAsset
        return isNewAssetPriceStoreFFEnabled().flatMapObservable { enabled ->
            if (enabled) {
                priceStore2.getCurrentPriceForAsset(base, quote).asObservable(errorMapper = ::toRxThrowable).map {
                    ExchangeRate(
                        from = base,
                        to = quote,
                        rate = it.rate
                    )
                }
            } else {
                priceStore.getPriceForAsset(base.networkTicker, quote.networkTicker).map {
                    ExchangeRate(
                        from = base,
                        to = quote,
                        rate = it.currentRate
                    )
                }
            }
        }.map {
            if (shouldInverse)
                it.inverse()
            else it
        }
    }

    override fun exchangeRateToUserFiat(fromAsset: Currency): Observable<ExchangeRate> =
        isNewAssetPriceStoreFFEnabled().flatMapObservable { enabled ->
            if (enabled) {
                priceStore2.getCurrentPriceForAsset(fromAsset, userFiat)
                    .asObservable(errorMapper = ::toRxThrowable)
                    .map {
                        ExchangeRate(
                            from = fromAsset,
                            to = userFiat,
                            rate = it.rate
                        )
                    }
            } else {
                priceStore.getPriceForAsset(fromAsset.networkTicker, userFiat.networkTicker).map {
                    ExchangeRate(
                        from = fromAsset,
                        to = userFiat,
                        rate = it.currentRate
                    )
                }
            }
        }

    override fun getLastCryptoToUserFiatRate(sourceCrypto: AssetInfo): ExchangeRate {
        val priceRate = if (isNewAssetPriceStoreFFEnabledCached) {
            priceStore2.getCachedAssetPrice(sourceCrypto, userFiat).rate
        } else {
            priceStore.getCachedAssetPrice(sourceCrypto, userFiat).currentRate
        }
        return ExchangeRate(
            from = sourceCrypto,
            to = userFiat,
            rate = priceRate
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
        val priceRate = if (isNewAssetPriceStoreFFEnabledCached) {
            priceStore2.getCachedAssetPrice(sourceCrypto, targetFiat).rate
        } else {
            priceStore.getCachedAssetPrice(sourceCrypto, targetFiat).currentRate
        }
        return ExchangeRate(
            from = sourceCrypto,
            to = targetFiat,
            rate = priceRate
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
                val priceRate = if (isNewAssetPriceStoreFFEnabledCached) {
                    priceStore2.getCachedFiatPrice(sourceFiat, userFiat).rate
                } else {
                    priceStore.getCachedFiatPrice(sourceFiat, userFiat).currentRate
                }
                return ExchangeRate(
                    from = sourceFiat,
                    to = userFiat,
                    rate = priceRate
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
        isNewAssetPriceStoreFFEnabled().flatMapObservable { enabled ->
            if (enabled) {
                Observable.combineLatest(
                    priceStore2.getCurrentPriceForAsset(fromAsset, fiat).asObservable(errorMapper = ::toRxThrowable),
                    priceStore2.getYesterdayPriceForAsset(fromAsset, fiat).asObservable(errorMapper = ::toRxThrowable)
                ) { current, yesterday ->
                    Prices24HrWithDelta(
                        delta24h = current.getPriceDelta(yesterday),
                        previousRate = ExchangeRate(
                            from = fromAsset,
                            to = fiat,
                            rate = yesterday.rate
                        ),
                        currentRate = ExchangeRate(
                            from = fromAsset,
                            to = fiat,
                            rate = current.rate
                        ),
                        marketCap = current.marketCap
                    )
                }
            } else {
                priceStore.getPriceForAsset(
                    fromAsset.networkTicker,
                    fiat.networkTicker
                ).map { price ->
                    Prices24HrWithDelta(
                        delta24h = price.priceDelta(),
                        previousRate = ExchangeRate(
                            from = fromAsset,
                            to = fiat,
                            rate = price.yesterdayRate
                        ),
                        currentRate = ExchangeRate(
                            from = fromAsset,
                            to = fiat,
                            rate = price.currentRate
                        ),
                        marketCap = price.marketCap
                    )
                }
            }
        }

    override fun getHistoricPriceSeries(
        asset: Currency,
        span: HistoricalTimeSpan,
        now: Calendar
    ): Single<HistoricalRateList> {
        require(asset.startDate != null)

        return isNewAssetPriceStoreFFEnabled().flatMap { enabled ->
            if (enabled) {
                rxSingleOutcome {
                    priceStore2.getHistoricalPriceForAsset(asset, userFiat, span)
                        .map { prices -> prices.map { it.toHistoricalRate() } }
                        .mapLeft(::toRxThrowable)
                }
            } else {
                val scale = span.suggestTimescaleInterval()
                val startTime = now.getStartTimeForTimeSpan(span, asset)

                assetPriceService.getHistoricPriceSeriesSince(
                    base = asset.networkTicker,
                    quote = userFiat.networkTicker,
                    start = startTime,
                    scale = scale
                ).toHistoricalRateList()
            }
        }
    }

    override fun get24hPriceSeries(
        asset: Currency
    ): Single<HistoricalRateList> =
        isNewAssetPriceStoreFFEnabled().flatMap { enabled ->
            if (enabled) {
                rxSingleOutcome {
                    priceStore2.getHistoricalPriceForAsset(asset, userFiat, HistoricalTimeSpan.DAY)
                        .map { prices -> prices.map { it.toHistoricalRate() } }
                        .mapLeft(::toRxThrowable)
                }
            } else {
                sparklineCall.fetch(asset, userFiat)
            }
        }

    override val fiatAvailableForRates: List<FiatCurrency>
        get() = if (isNewAssetPriceStoreFFEnabledCached) {
            priceStore2.fiatQuoteTickers.mapNotNull {
                assetCatalogue.fiatFromNetworkTicker(it)
            }
        } else {
            priceStore.fiatQuoteTickers.mapNotNull {
                assetCatalogue.fiatFromNetworkTicker(it)
            }
        }

    private fun toRxThrowable(error: AssetPriceError): Throwable = when (error) {
        is AssetPriceError.PricePairNotFound -> AssetPriceNotCached(error.base.networkTicker, error.quote.networkTicker)
        is AssetPriceError.RequestFailed -> Exception(error.message)
    }

    private fun AssetPriceRecord2.getPriceDelta(other: AssetPriceRecord2): Double {
        val thisRate = this.rate
        val otherRate = other.rate
        return try {
            when {
                otherRate == null || thisRate == null -> Double.NaN
                otherRate.signum() != 0 -> {
                    (thisRate - otherRate)
                        .divide(otherRate, 4, RoundingMode.HALF_EVEN)
                        .movePointRight(2)
                        .toDouble()
                }
                else -> Double.NaN
            }
        } catch (t: ArithmeticException) {
            Double.NaN
        }
    }

    private fun AssetPriceRecord2.toHistoricalRate(): HistoricalRate =
        HistoricalRate(this.fetchedAt.toSeconds(), this.rate?.toDouble() ?: 0.0)
}
