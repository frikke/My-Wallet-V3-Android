package com.blockchain.core.price

import com.blockchain.core.price.model.AssetPriceRecord
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.common.model.Seconds
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.Calendar
import kotlinx.coroutines.flow.Flow

enum class HistoricalTimeSpan(val value: Int) {
    DAY(0),
    WEEK(1),
    MONTH(2),
    YEAR(3),
    ALL_TIME(4);

    companion object {
        fun fromValue(value: Int) = values().first { it.value == value }
    }
}

data class HistoricalRate(
    val timestamp: Seconds,
    val rate: Double,
)

typealias HistoricalRateList = List<HistoricalRate>

data class Prices24HrWithDelta(
    val delta24h: Double,
    val previousRate: ExchangeRate,
    val currentRate: ExchangeRate,
    val marketCap: Double? = null,
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
    fun init(): Completable

    @Deprecated("Use the reactive flow exchangeRate")
    fun exchangeRateLegacy(fromAsset: Currency, toAsset: Currency): Observable<ExchangeRate>

    fun exchangeRate(
        fromAsset: Currency,
        toAsset: Currency,
    ): Flow<DataResource<ExchangeRate>>

    fun exchangeRateToUserFiat(
        fromAsset: Currency,
    ): Observable<ExchangeRate>

    fun exchangeRateToUserFiatFlow(
        fromAsset: Currency,
    ): Flow<DataResource<ExchangeRate>>

    fun getHistoricRate(fromAsset: Currency, secSinceEpoch: Long): Single<ExchangeRate>

    @Deprecated("Use the reactive getPricesWith24hDelta")
    fun getPricesWith24hDeltaLegacy(
        fromAsset: Currency
    ): Observable<Prices24HrWithDelta>

    @Deprecated("Use the reactive getPricesWith24hDelta")
    fun getPricesWith24hDeltaLegacy(
        fromAsset: Currency,
        fiat: Currency
    ): Observable<Prices24HrWithDelta>

    fun getPricesWith24hDelta(
        fromAsset: Currency
    ): Flow<DataResource<Prices24HrWithDelta>>

    fun getHistoricPriceSeries(
        asset: Currency,
        span: HistoricalTimeSpan,
        now: Calendar = Calendar.getInstance(),
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Flow<DataResource<HistoricalRateList>>

    // Specialised call to historic rates for sparkline caching
    fun get24hPriceSeries(
        asset: Currency,
    ): Flow<DataResource<HistoricalRateList>>

    fun getCurrentAssetPrice(
        asset: Currency,
        fiat: Currency
    ): Single<AssetPriceRecord>

    val fiatAvailableForRates: List<FiatCurrency>
}
