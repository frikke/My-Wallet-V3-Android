package com.blockchain.core.price

import com.blockchain.core.price.impl.SupportedTickerList
import info.blockchain.balance.AssetInfo
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
    fun getLastCryptoToUserFiatRate(sourceCrypto: AssetInfo): ExchangeRate.CryptoToFiat
    @Deprecated("User the reactive get operations")
    fun getLastFiatToUserFiatRate(sourceFiat: String): ExchangeRate.FiatToFiat
    @Deprecated("User the reactive get operations")
    fun getLastCryptoToFiatRate(sourceCrypto: AssetInfo, targetFiat: String): ExchangeRate.CryptoToFiat
    @Deprecated("User the reactive get operations")
    fun getLastFiatToFiatRate(sourceFiat: String, targetFiat: String): ExchangeRate.FiatToFiat
    @Deprecated("User the reactive get operations")
    fun getLastFiatToCryptoRate(sourceFiat: String, targetCrypto: AssetInfo): ExchangeRate.FiatToCrypto
}

interface ExchangeRatesDataManager : ExchangeRates {
    fun init(): Single<SupportedTickerList>

    fun cryptoToFiatRate(fromAsset: AssetInfo, toFiat: String): Observable<ExchangeRate>
    fun fiatToFiatRate(fromFiat: String, toFiat: String): Observable<ExchangeRate>
    fun cryptoToUserFiatRate(fromAsset: AssetInfo): Observable<ExchangeRate>
    fun fiatToUserFiatRate(fromFiat: String): Observable<ExchangeRate>

    fun getHistoricRate(fromAsset: AssetInfo, secSinceEpoch: Long): Single<ExchangeRate>
    fun getPricesWith24hDelta(fromAsset: AssetInfo): Observable<Prices24HrWithDelta>
    fun getPricesWith24hDelta(fromAsset: AssetInfo, fiat: String): Observable<Prices24HrWithDelta>

    fun getHistoricPriceSeries(
        asset: AssetInfo,
        span: HistoricalTimeSpan,
        now: Calendar = Calendar.getInstance()
    ): Single<HistoricalRateList>

    // Specialised call to historic rates for sparkline caching
    fun get24hPriceSeries(
        asset: AssetInfo
    ): Single<HistoricalRateList>

    val fiatAvailableForRates: List<String>
}
