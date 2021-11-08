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
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import timber.log.Timber

internal data class AssetPriceRecord(
    val base: String,
    val quote: String,
    val currentRate: BigDecimal? = null,
    val yesterdayRate: BigDecimal? = null,
    val fetchedAtMillis: Long
) {
    fun priceDelta(): Double =
        try {
            when {
                yesterdayRate == null || currentRate == null -> Double.NaN
                yesterdayRate.signum() != 0 -> {
                    (currentRate - yesterdayRate)
                        .divide(yesterdayRate, 4, RoundingMode.HALF_EVEN)
                        .movePointRight(2)
                        .toDouble()
                }
                else -> Double.NaN
            }
        } catch (t: ArithmeticException) {
            Double.NaN
        }
}

private data class AssetPair(
    val base: String,
    val quote: String
)

private class AssetPriceNotCached(pair: AssetPair) :
    Throwable("No cached price available for ${pair.base} to ${pair.quote}")

private typealias AssetPricesMap = MutableMap<AssetPair, AssetPriceRecord>
// TEMP to get supported user fiats. This should come from the dynamic endpoints and
// asset catalogue once that is implemented in the client
typealias SupportedTickerList = List<String>

// The price api BE refreshes it's cache every minute, so there's no point refreshing ours more than that.
internal class AssetPriceStore(
    private val assetPriceService: AssetPriceService,
    private val assetCatalogue: AssetCatalogue,
    private val prefs: CurrencyPrefs
) {
    private lateinit var supportedBaseTickers: SupportedTickerList
    private lateinit var supportedQuoteTickers: SupportedTickerList
    private lateinit var supportedFiatQuoteTickers: SupportedTickerList

    private val pricesCache: BehaviorRelay<AssetPricesMap> =
        BehaviorRelay.createDefault(mutableMapOf())

    private fun targetQuoteList(): List<String> =
        assetCatalogue.supportedFiatAssets + prefs.selectedFiatCurrency

    fun init(): Single<SupportedTickerList> {
        return assetPriceService.getSupportedCurrencies()
            .doOnSuccess { symbols ->
                supportedBaseTickers = symbols.base.map { it.ticker }
                supportedQuoteTickers = symbols.quote.map { it.ticker }
                supportedFiatQuoteTickers = symbols.quote.filter { it.isFiat }.map { it.ticker }
            }.map {
                supportedFiatQuoteTickers
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
        if (tempMap.isNotEmpty()) {
            val current = tempMap.values.first().fetchedAtMillis / 1000
            val yesterday = current - SECONDS_PER_DAY
            assetPriceService.getHistoricPrices(
                baseTickers = baseTickerSet,
                quoteTickers = quoteTickerSet,
                time = yesterday
            ).map { yesterdayPrices ->
                updateCachedDataWithYesterdayPrice(tempMap, yesterdayPrices)
            }
        } else {
            Single.just(tempMap)
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
        pricesCache.value?.let { currentMap ->
            if (currentMap.isNotEmpty()) {
                // Update as usual
                currentMap.putAll(tempMap)
                pricesCache.accept(currentMap)
            } else {
                // Accept the new data as is if the existing one is empty
                pricesCache.accept(tempMap)
            }
        } ?: kotlin.run {
            // Accept the new data as is if the existing one is null
            pricesCache.accept(tempMap)
        }
    }

    @Synchronized
    private fun lookupCachedPrice(assetPair: AssetPair, map: AssetPricesMap): AssetPriceRecord {
        return if (assetPair.base == assetPair.quote) {
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
                    Timber.e("Unable to get prices: $t")
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
        val pair = AssetPair(fromAsset.networkTicker, toFiat)
        return pricesCache.value?.get(pair) ?: throw IllegalStateException(
            "Unknown pair: ${fromAsset.networkTicker} - $toFiat"
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
        pricesCache.value?.filterKeys { k -> k.base !in stale }?.toMutableMap()?.let { filteredMap ->
            requestBuffer.addPendingBatch(stale)
            if (filteredMap.isNotEmpty()) {
                // Do not replace the existing data with the new one if it's empty
                pricesCache.accept(filteredMap)
            }
        }
    }

    private fun getStalePriceBases(): Set<String> {
        val now = System.currentTimeMillis()
        return pricesCache.value?.values?.filter { it.isStale(now) }?.map { it.base }?.toSet() ?: emptySet()
    }

    val fiatQuoteTickers: SupportedTickerList
        get() = supportedFiatQuoteTickers

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
        currentRate = if (this.price.isNaN()) null else this.price.toBigDecimal(),
        fetchedAtMillis = nowMillis
    )

private fun AssetPriceRecord.updateYesterdayPrice(price: AssetPrice): AssetPriceRecord =
    this.copy(
        yesterdayRate = if (price.price.isNaN()) null else price.price.toBigDecimal()
    )

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
