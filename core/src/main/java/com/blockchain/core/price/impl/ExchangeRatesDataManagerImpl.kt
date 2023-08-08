package com.blockchain.core.price.impl

import com.blockchain.api.services.AssetPriceService
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.HistoricalRate
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.core.price.impl.assetpricestore.AssetPriceStore
import com.blockchain.core.price.model.AssetPriceNotFoundException
import com.blockchain.core.price.model.AssetPriceRecord
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import com.blockchain.data.firstOutcome
import com.blockchain.data.mapData
import com.blockchain.data.mapError
import com.blockchain.data.toObservable
import com.blockchain.domain.common.model.toSeconds
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.utils.rxCompletableOutcome
import com.blockchain.utils.rxSingleOutcome
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.RoundingMode
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class ExchangeRatesDataManagerImpl(
    private val priceStore: AssetPriceStore,
    private val assetPriceService: AssetPriceService,
    private val assetCatalogue: AssetCatalogue,
    private val currencyPrefs: CurrencyPrefs
) : ExchangeRatesDataManager {

    private val userFiat: Currency
        get() = currencyPrefs.selectedFiatCurrency

    override fun init(): Completable = rxCompletableOutcome {
        priceStore.warmSupportedTickersCache()
    }

    override fun getCurrentAssetPrice(
        asset: Currency,
        fiat: Currency
    ): Single<AssetPriceRecord> =
        rxSingleOutcome {
            priceStore.getCurrentPriceForAsset(asset, fiat)
                .firstOutcome()
        }

    override fun exchangeRate(
        fromAsset: Currency,
        toAsset: Currency
    ): Flow<DataResource<ExchangeRate>> {
        val shouldInverse = fromAsset.type == CurrencyType.FIAT && toAsset.type == CurrencyType.CRYPTO
        val base = if (shouldInverse) toAsset else fromAsset
        val quote = if (shouldInverse) fromAsset else toAsset
        return priceStore.getCurrentPriceForAsset(base, quote)
            .mapData {
                ExchangeRate(
                    from = base,
                    to = quote,
                    rate = it.rate
                ).apply {
                    if (shouldInverse) {
                        inverse()
                    }
                }
            }
    }

    override fun exchangeRateLegacy(fromAsset: Currency, toAsset: Currency): Observable<ExchangeRate> {
        val shouldInverse = fromAsset.type == CurrencyType.FIAT && toAsset.type == CurrencyType.CRYPTO
        val base = if (shouldInverse) toAsset else fromAsset
        val quote = if (shouldInverse) fromAsset else toAsset
        return priceStore.getCurrentPriceForAsset(base, quote)
            .toObservable().map {
                ExchangeRate(
                    from = base,
                    to = quote,
                    rate = it.rate
                )
            }.map {
                if (shouldInverse) {
                    it.inverse()
                } else {
                    it
                }
            }
    }

    override fun exchangeRateToUserFiat(
        fromAsset: Currency
    ): Observable<ExchangeRate> =
        priceStore.getCurrentPriceForAsset(fromAsset, userFiat)
            .toObservable()
            .map {
                ExchangeRate(
                    from = fromAsset,
                    to = userFiat,
                    rate = it.rate
                )
            }

    override fun exchangeRateToUserFiatFlow(
        fromAsset: Currency
    ): Flow<DataResource<ExchangeRate>> {
        return priceStore
            .getCurrentPriceForAsset(
                base = fromAsset,
                quote = userFiat
            )
            .mapData {
                ExchangeRate(
                    from = fromAsset,
                    to = userFiat,
                    rate = it.rate
                )
            }
    }

    override fun getLastCryptoToUserFiatRate(sourceCrypto: AssetInfo): ExchangeRate {
        val priceRate = priceStore.getCachedAssetPrice(sourceCrypto, userFiat).rate
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
        val priceRate = priceStore.getCachedAssetPrice(sourceCrypto, targetFiat).rate
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
                val priceRate = priceStore.getCachedFiatPrice(sourceFiat, userFiat).rate
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

    override fun getPricesWith24hDeltaLegacy(
        fromAsset: Currency
    ): Observable<Prices24HrWithDelta> =
        getPricesWith24hDeltaLegacy(fromAsset, userFiat)

    override fun getPricesWith24hDeltaLegacy(
        fromAsset: Currency,
        fiat: Currency
    ): Observable<Prices24HrWithDelta> = Observable.combineLatest(
        priceStore.getCurrentPriceForAsset(fromAsset, fiat)
            .toObservable(),
        priceStore.getYesterdayPriceForAsset(fromAsset, fiat)
            .toObservable()
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

    override fun getPricesWith24hDelta(
        fromAsset: Currency
    ): Flow<DataResource<Prices24HrWithDelta>> {
        return combine(
            priceStore.getCurrentPriceForAsset(
                base = fromAsset,
                quote = userFiat
            ),
            priceStore.getYesterdayPriceForAsset(
                base = fromAsset,
                quote = userFiat
            )
        ) { currentPrice, yesterdayPrice ->
            combineDataResources(currentPrice, yesterdayPrice) { currentPriceData, yesterdayPriceData ->
                Prices24HrWithDelta(
                    delta24h = currentPriceData.getPriceDelta(yesterdayPriceData),
                    previousRate = ExchangeRate(
                        from = fromAsset,
                        to = userFiat,
                        rate = yesterdayPriceData.rate
                    ),
                    currentRate = ExchangeRate(
                        from = fromAsset,
                        to = userFiat,
                        rate = currentPriceData.rate
                    ),
                    marketCap = currentPriceData.marketCap
                )
            }
        }
    }

    override fun getHistoricPriceSeries(
        asset: Currency,
        span: HistoricalTimeSpan,
        now: Calendar
    ): Flow<DataResource<HistoricalRateList>> {
        require(asset.startDate != null)
        return priceStore.getHistoricalPriceForAsset(asset, userFiat, span)
            .mapData { prices -> prices.map { it.toHistoricalRate() } }
            .mapError { AssetPriceNotFoundException(asset.networkTicker, userFiat.networkTicker) }
    }

    override fun get24hPriceSeries(
        asset: Currency
    ): Flow<DataResource<HistoricalRateList>> =
        priceStore.getHistoricalPriceForAsset(asset, userFiat, HistoricalTimeSpan.DAY)
            .mapData { prices -> prices.map { it.toHistoricalRate() } }
            .mapError { AssetPriceNotFoundException(asset.networkTicker, userFiat.networkTicker) }

    override val fiatAvailableForRates: List<FiatCurrency>
        get() = priceStore.fiatQuoteTickers.mapNotNull {
            assetCatalogue.fiatFromNetworkTicker(it)
        }

    private fun AssetPriceRecord.getPriceDelta(other: AssetPriceRecord): Double {
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

    private fun AssetPriceRecord.toHistoricalRate(): HistoricalRate =
        HistoricalRate(this.fetchedAt.toSeconds(), this.rate?.toDouble() ?: 0.0)
}
