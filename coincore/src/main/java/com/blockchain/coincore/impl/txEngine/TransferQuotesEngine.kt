package com.blockchain.coincore.impl.txEngine

import com.blockchain.core.custodial.BrokerageDataManager
import com.blockchain.core.custodial.models.BrokerageQuote
import com.blockchain.domain.common.model.Millis
import com.blockchain.domain.common.model.toSeconds
import com.blockchain.domain.trade.TradeDataService
import com.blockchain.domain.trade.model.QuotePrice
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.repositories.QuotesProvider
import com.blockchain.outcome.Outcome
import com.blockchain.utils.CurrentTimeProvider
import com.blockchain.utils.awaitOutcome
import com.blockchain.utils.rxSingleOutcome
import com.blockchain.utils.toObservable
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

class TransferQuotesEngine private constructor(
    private val quotesProvider: QuotesProvider,
    private val custodialWalletManager: CustodialWalletManager,
    private val brokerageDataManager: BrokerageDataManager,
    private val tradeDataService: TradeDataService,
    private val sellSwapBrokerageQuoteFF: FeatureFlag,
) {
    private val latestPrice = BehaviorSubject.create<QuotePrice>()
    private val latestQuote = BehaviorSubject.create<PricedQuote>()

    private lateinit var product: Product
    private lateinit var direction: TransferDirection
    private lateinit var pair: CurrencyPair
    private val quoteRefreshTimes = mutableListOf<Long>()

    private val stopQuoteRefreshing = PublishSubject.create<Unit>()
    private val stopPriceRefreshing = PublishSubject.create<Unit>()

    private val amount by lazy {
        BehaviorSubject.createDefault(Money.zero(pair.source))
    }

    private var priceObservable: Observable<QuotePrice>? = null
    private var quoteObservable: Observable<PricedQuote>? = null

    fun getPriceExchangeRate(): Observable<ExchangeRate> = getPriceQuote().map {
        it.sourceToDestinationRate
    }

    private val quoteExchangeRate = latestQuote.map {
        it.transferQuote.sourceToDestinationRate
    }.replay().refCount()
    fun getQuoteExchangeRate(): Observable<ExchangeRate> = quoteExchangeRate

    fun getPriceQuote(): Observable<QuotePrice> {
        return priceObservable
            ?: amount.switchMap { amount ->
                val getPrice = sellSwapBrokerageQuoteFF.enabled.flatMap { enabled ->
                    if (enabled) {
                        when (product) {
                            Product.SELL -> rxSingleOutcome {
                                tradeDataService.getSellQuotePrice(pair, amount, direction)
                            }
                            Product.TRADE -> rxSingleOutcome {
                                tradeDataService.getSwapQuotePrice(pair, amount, direction)
                            }
                            else -> throw UnsupportedOperationException()
                        }
                    } else {
                        quotesProvider.fetchPrice(amount, direction, pair)
                    }
                }
                Observable.interval(
                    0L,
                    PRICE_QUOTE_REFRESH,
                    TimeUnit.MILLISECONDS
                ).flatMapSingle {
                    getPrice
                }
            }.doFinally {
                priceObservable = null
            }.doOnNext {
                latestPrice.onNext(it)
            }.takeUntil(stopPriceRefreshing)
                .replay(1).refCount().also {
                    priceObservable = it
                }
    }
    private fun fetchQuote(): Observable<QuoteWithRefreshInterval> =
        amount.firstOrError().flatMapObservable { amount ->
            flow {
                while (true) {
                    val getQuote = sellSwapBrokerageQuoteFF.enabled.flatMap { enabled ->
                        if (enabled) {
                            when (product) {
                                Product.SELL -> brokerageDataManager.getSellQuote(pair, amount, direction)
                                Product.TRADE -> rxSingleOutcome {
                                    // TODO(aromano): SWAP check UxError
                                    brokerageDataManager.getSwapQuote(pair, amount, direction)
                                }
                                else -> throw UnsupportedOperationException()
                            }
                        } else {
                            quotesProvider.fetchQuote(amount, direction, pair)
                        }
                    }

                    when (val result = getQuote.awaitOutcome()) {
                        is Outcome.Success -> {
                            val quote = result.value
                            val interval = min(quote.millisToExpire(), MIN_QUOTE_REFRESH)
                            val refreshTime = CurrentTimeProvider.currentTimeMillis() + interval

                            val data = QuoteWithRefreshInterval(
                                quote = quote,
                                refreshTotalWaitTime = interval,
                                scheduledRefreshTime = refreshTime,
                            )
                            emit(Outcome.Success(data))
                            delay(interval)
                        }
                        is Outcome.Failure -> emit(result)
                    }
                }
            }.toObservable()
        }.takeUntil(stopQuoteRefreshing)

    fun getQuote(): Observable<PricedQuote> {
        return quoteObservable ?: let {
            val ticker = Observable.interval(TIMER_DELAY, TIMER_DELAY, TimeUnit.SECONDS)
            Observables.combineLatest(fetchQuote(), ticker).map { (quoteWithRefreshInterval, _) ->
                val remainingMillis =
                    (quoteWithRefreshInterval.scheduledRefreshTime - CurrentTimeProvider.currentTimeMillis())
                        .coerceAtLeast(0)

                val remainingPercentage = remainingMillis.toFloat() / quoteWithRefreshInterval.refreshTotalWaitTime

                PricedQuote(
                    quoteWithRefreshInterval.quote,
                    remainingMillis.toSeconds().toInt(),
                    remainingPercentage,
                )
            }.doFinally {
                quoteObservable = null
            }.doOnNext {
                latestQuote.onNext(it)
            }.takeUntil(stopQuoteRefreshing)
                .replay().refCount().also {
                    quoteObservable = it
                }
        }
    }

    fun getSampleDepositAddress(): Single<String> =
        sellSwapBrokerageQuoteFF.enabled.flatMap { enabled ->
            if (enabled) {
                custodialWalletManager.getCustodialAccountAddress(product, pair.source)
            } else {
                quotesProvider.getSampleDepositAddress(direction, pair)
            }
        }

    fun start(
        product: Product,
        direction: TransferDirection,
        pair: CurrencyPair
    ) {
        stop()
        this.product = product
        this.direction = direction
        this.pair = pair
        amount.onNext(Money.zero(pair.source))
    }

    fun stop() {
        stopQuoteRefreshing.onNext(Unit)
        stopPriceRefreshing.onNext(Unit)
        quoteRefreshTimes.clear()
        priceObservable = null
        quoteObservable = null
    }

    fun getLatestPrice(): QuotePrice = latestPrice.value!!

    fun getLatestQuote(): PricedQuote = latestQuote.value!!

    fun updateAmount(newAmount: Money) {
        amount.onNext(newAmount)
        stopQuoteRefreshing.onNext(Unit)
    }

    companion object {
        const val PRICE_QUOTE_REFRESH: Long = 10_000L
        const val MIN_QUOTE_REFRESH: Long = 90000L
        private const val TIMER_DELAY: Long = 1
    }

    class Factory(
        private val quotesProvider: QuotesProvider,
        private val custodialWalletManager: CustodialWalletManager,
        private val brokerageDataManager: BrokerageDataManager,
        private val tradeDataService: TradeDataService,
        private val sellSwapBrokerageQuoteFF: FeatureFlag,
    ) {
        fun create(): TransferQuotesEngine = TransferQuotesEngine(
            quotesProvider,
            custodialWalletManager,
            brokerageDataManager,
            tradeDataService,
            sellSwapBrokerageQuoteFF,
        )
    }
}

data class PricedQuote(
    val transferQuote: BrokerageQuote,
    val remainingSeconds: Int,
    val remainingPercentage: Float
)

private data class QuoteWithRefreshInterval(
    val quote: BrokerageQuote,
    val refreshTotalWaitTime: Millis,
    val scheduledRefreshTime: Millis,
)
