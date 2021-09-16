package com.blockchain.core.price.impl

import com.blockchain.api.services.AssetPrice
import com.blockchain.api.services.AssetPriceService
import com.blockchain.preferences.CurrencyPrefs
import com.jakewharton.rxrelay3.BehaviorRelay
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

internal data class AssetPriceRecord(
    val base: String,
    val quote: String,
    val currentRate: BigDecimal,
    val yesterdayRate: BigDecimal = 0.0.toBigDecimal(),
    val fetchedAtMillis: Long
) {
    fun priceDelta(): Double =
        try {
            if (yesterdayRate.signum() != 0) {
                (currentRate - yesterdayRate)
                    .divide(yesterdayRate, 4, RoundingMode.HALF_EVEN)
                    .movePointRight(2)
                    .toDouble()
            } else {
                Double.NaN
            }
        } catch (t: ArithmeticException) {
            Double.NaN
        }
}

private data class AssetPair(
    val base: String,
    val quote: String
)

private class AssetPriceNotCached(pair: AssetPair)
    : Throwable("No cached price available for ${pair.base} to ${pair.quote}")

private typealias AssetPricesMap = MutableMap<AssetPair, AssetPriceRecord>
// TEMP to get supported user fiats. This should come from the dynamic endpoints and
// asset catalogue once that is implemented in the client
typealias SupportedFiatTickerList = List<String>

// The price api BE refreshes it's cache every minute, so there's no point refreshing ours more than that.
internal class AssetPriceStore(
    private val assetPriceService: AssetPriceService,
    private val assetCatalogue: AssetCatalogue,
    private val prefs: CurrencyPrefs
) {
    private lateinit var supportedUserFiat: SupportedFiatTickerList

    private val pricesCache: BehaviorRelay<AssetPricesMap> =
        BehaviorRelay.createDefault(mutableMapOf())

    private fun targetQuoteList(): List<String> =
        assetCatalogue.supportedFiatAssets + prefs.selectedFiatCurrency

    fun init(): Single<SupportedFiatTickerList> {
        return assetPriceService.getSupportedCurrencies()
            .doOnSuccess { symbols ->
                supportedUserFiat = symbols.quote.filter { it.isFiat }.map { it.ticker }
            }.map {
                supportedUserFiat
            }
    }

    private fun loadAndCachePrices(
        baseTickerSet: Set<String>,
        quoteTickerSet: Set<String>
    ) = assetPriceService.getCurrentPrices(
        baseTickerList = baseTickerSet,
        quoteTickerList = quoteTickerSet
    ).map { pricesNow ->
        buildTempPriceMap(pricesNow)
    }.flatMap { tempMap ->
        val current = tempMap.values.first().fetchedAtMillis / 1000
        val yesterday = current - SECONDS_PER_DAY
        assetPriceService.getHistoricPrices(
            baseTickers = baseTickerSet,
            quoteTickers = quoteTickerSet,
            time = yesterday
        ).map { yesterdayPrices ->
            updateCachedDataWithYesterdayPrice(tempMap, yesterdayPrices)
        }
    }.doOnSuccess { tempMap ->
        updatePriceMapCache(tempMap)
    }.doOnError {
        Timber.e("Failed to get prices: $it")
    }

    private fun buildTempPriceMap(prices: List<AssetPrice>): AssetPricesMap {
        val now = System.currentTimeMillis()
        val priceMap: AssetPricesMap = mutableMapOf()
        prices.forEach {
            val pair = AssetPair(it.base, it.quote)
            priceMap[pair] = it.toAssetPriceRecord(now)
        }
        return priceMap
    }

    private fun updateCachedDataWithYesterdayPrice(
        priceMap: AssetPricesMap,
        prices: List<AssetPrice>
    ): AssetPricesMap {
        prices.forEach {
            val pair = AssetPair(it.base, it.quote)
            priceMap[pair]?.let { record ->
                priceMap[pair] = record.updateYesterdayPrice(it)
            }
        }
        return priceMap
    }

    @Synchronized
    private fun updatePriceMapCache(tempMap: AssetPricesMap) {
        pricesCache.value?.let {
            it.putAll(tempMap)
            pricesCache.accept(it)
        }
    }

    @Synchronized
    private fun lookupCachedPrice(assetPair: AssetPair, map: AssetPricesMap): AssetPriceRecord =
        if (assetPair.base == assetPair.quote) {
            AssetPriceRecord(
                base = assetPair.base,
                quote = assetPair.quote,
                currentRate = 1.0.toBigDecimal(),
                yesterdayRate = 1.0.toBigDecimal(),
                fetchedAtMillis = System.currentTimeMillis()
            )
        } else {
            map[assetPair] ?: throw AssetPriceNotCached(assetPair)
        }

    internal fun getPriceForAsset(base: String, quote: String): Observable<AssetPriceRecord> =
        pricesCache.map {
            lookupCachedPrice(
                AssetPair(base, quote),
                it
            )
        }.map {
            it.takeIf { !it.isStale() } ?: throw AssetPriceNotCached(AssetPair(base, quote))
        }.doOnSubscribe {
            checkStartRefreshTimer()
        }.doOnDispose {
            checkStopRefreshTimer()
        }.retryWhen { errors ->
            errors.flatMap { t ->
                if (t is AssetPriceNotCached) {
                    Observable.timer(RETRY_BATCH_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                        .flatMap { fetchNewPrices(base) }
                } else {
                    Observable.error(t)
                }
            }
        }.distinctUntilChanged()

    private val requestBuffer = PendingRequestBuffer()

    @Synchronized
    private fun fetchNewPrices(base: String): Observable<Int> {
        val nextReq = requestBuffer.getNextRequestElements(base)
        return if (nextReq.isEmpty()) {
            Observable.just(FETCH_NOT_REQUIRED_VALUE_IGNORED)
        } else {
            loadAndCachePrices(
                baseTickerSet = nextReq,
                quoteTickerSet = targetQuoteList().toSet()
            ).doOnSuccess {
                requestBuffer.requestComplete()
            }.map { FETCH_REQUIRED_VALUE_IGNORED }
            .toObservable()
        }
    }

    @Synchronized
    fun getCachedAssetPrice(fromAsset: AssetInfo, toFiat: String): AssetPriceRecord {
        val pair = AssetPair(fromAsset.ticker, toFiat)
        return pricesCache.value?.get(pair) ?: throw IllegalStateException(
            "Unknown pair: ${fromAsset.ticker} - $toFiat"
        )
    }

    @Synchronized
    fun getCachedFiatPrice(fromFiat: String, toFiat: String): AssetPriceRecord {
        val pair = AssetPair(fromFiat, toFiat)
        return pricesCache.value?.get(pair) ?: throw IllegalStateException("Unknown pair: $fromFiat - $toFiat")
    }

    private var liveStaleCheck: Disposable? = null

    @Synchronized
    private fun checkStartRefreshTimer() {
        if (pricesCache.hasObservers() && liveStaleCheck == null) {
            liveStaleCheck = Observable.interval(
                CACHE_REFRESH_DELAY_MILLIS,
                CACHE_REFRESH_DELAY_MILLIS,
                TimeUnit.MILLISECONDS,
                Schedulers.io()
            ).subscribeBy(
                onNext = {
                    refreshStalePrices()
                }
            )
        }
    }

    @Synchronized
    private fun checkStopRefreshTimer() {
        if (!pricesCache.hasObservers()) {
            liveStaleCheck?.dispose()
            liveStaleCheck = null
        }
    }

    @Synchronized
    private fun refreshStalePrices() {
        val stale = getStalePriceBases()
        pricesCache.value?.filterKeys { k -> k.base !in stale }?.toMutableMap()?.let {
            requestBuffer.addPendingBatch(stale)
            pricesCache.accept(it)
        }
    }

    private fun getStalePriceBases(): Set<String> {
        val now = System.currentTimeMillis()
        return pricesCache.value?.values?.filter { it.isStale(now) }?.map { it.base }?.toSet() ?: emptySet()
    }

    val fiatQuoteTickers: SupportedFiatTickerList
        get() = supportedUserFiat

    companion object {
        private const val CACHE_STALE_AGE_MILLIS = 90 * 1000L

        private fun AssetPriceRecord.isStale(now: Long) =
            this.fetchedAtMillis + CACHE_STALE_AGE_MILLIS < now

        private fun AssetPriceRecord.isStale() =
            this.fetchedAtMillis + CACHE_STALE_AGE_MILLIS < System.currentTimeMillis()

        internal const val CACHE_REFRESH_DELAY_MILLIS = 30 * 1000L

        private const val RETRY_BATCH_DELAY_MILLIS = 200L
        private const val SECONDS_PER_DAY = 24 * 60 * 60

        private const val FETCH_NOT_REQUIRED_VALUE_IGNORED = 0
        private const val FETCH_REQUIRED_VALUE_IGNORED = 1
    }
}

private fun AssetPrice.toAssetPriceRecord(nowMillis: Long) =
    AssetPriceRecord(
        base = this.base,
        quote = this.quote,
        currentRate = this.price.toBigDecimal(),
        fetchedAtMillis = nowMillis
    )

private fun AssetPriceRecord.updateYesterdayPrice(price: AssetPrice): AssetPriceRecord =
    this.copy(yesterdayRate = price.price.toBigDecimal())

private class PendingRequestBuffer {

    private val pendingRequest = mutableSetOf<String>()
    private val executingRequest = mutableSetOf<String>()

    @Synchronized
    fun getNextRequestElements(element: String): Set<String> =
        if (executingRequest.isNotEmpty()) {
            if (!executingRequest.contains(element)) {
                pendingRequest.add(element)
            }

            emptySet()
        } else {
            executingRequest.addAll(pendingRequest)
            executingRequest.add(element)
            pendingRequest.clear()

            Timber.d("Clean execution, new Request: $executingRequest")
            executingRequest.toSet()
        }

    @Synchronized
    fun addPendingBatch(bases: Set<String>) {
        pendingRequest.addAll(bases)
    }

    @Synchronized
    fun requestComplete() {
        Timber.d("Clear execution cache")
        executingRequest.clear()
    }
}