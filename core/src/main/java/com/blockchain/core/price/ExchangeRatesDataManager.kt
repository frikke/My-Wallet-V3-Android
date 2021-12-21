package com.blockchain.core.price

import com.blockchain.core.price.impl.SupportedTickerList
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.Calendar

enum class HistoricalTimeSpan {
    DAY,
    WEEK,
    MONTH,
    YEAR,
    ALL_TIME
}

data class HistoricalRate(
    val timestamp: Long,
    val rate: Double
)

typealias HistoricalRateList = List<HistoricalRate>

data class Prices24HrWithDelta(
    val delta24h: Double,
    val previousRate: ExchangeRate,
    val currentRate: ExchangeRate
)

interface ExchangeRates {
    @Deprecated("User the reactive get operations")
    fun getLastCryptoToUserFiatRate(sourceCrypto: AssetInfo): ExchangeRate

    @Deprecated("User the reactive get operations")
    fun getLastFiatToUserFiatRate(sourceFiat: FiatCurrency): ExchangeRate

    @Deprecated("User the reactive get operations")
    fun getLastCryptoToFiatRate(sourceCrypto: AssetInfo, targetFiat: FiatCurrency): ExchangeRate

    @Deprecated("User the reactive get operations")
    fun getLastFiatToFiatRate(sourceFiat: FiatCurrency, targetFiat: FiatCurrency): ExchangeRate

    @Deprecated("User the reactive get operations")
    fun getLastFiatToCryptoRate(sourceFiat: FiatCurrency, targetCrypto: AssetInfo): ExchangeRate
}

interface ExchangeRatesDataManager : ExchangeRates {
    fun init(): Single<SupportedTickerList>

    fun exchangeRate(fromAsset: Currency, toAsset: Currency): Observable<ExchangeRate>
    fun exchangeRateToUserFiat(fromAsset: Currency): Observable<ExchangeRate>

    fun getHistoricRate(fromAsset: Currency, secSinceEpoch: Long): Single<ExchangeRate>
    fun getPricesWith24hDelta(fromAsset: Currency): Observable<Prices24HrWithDelta>
    fun getPricesWith24hDelta(fromAsset: Currency, fiat: Currency): Observable<Prices24HrWithDelta>

    fun getHistoricPriceSeries(
        asset: Currency,
        span: HistoricalTimeSpan,
        now: Calendar = Calendar.getInstance()
    ): Single<HistoricalRateList>

    // Specialised call to historic rates for sparkline caching
    fun get24hPriceSeries(
        asset: Currency
    ): Single<HistoricalRateList>

    val fiatAvailableForRates: List<FiatCurrency>
}
