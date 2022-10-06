package com.blockchain.coincore.impl.txEngine

import com.blockchain.coincore.impl.PricesInterpolator
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.TransferQuote
import com.blockchain.nabu.datamanagers.repositories.QuotesProvider
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.math.min

class TransferQuotesEngine(
    private val quotesProvider: QuotesProvider
) {
    private lateinit var latestQuote: PricedQuote

    private lateinit var direction: TransferDirection
    private lateinit var pair: CurrencyPair
    private val quoteRefreshTimes = mutableListOf<Long>()

    private val stop = PublishSubject.create<Unit>()

    private val amount
        get() = BehaviorSubject.createDefault(Money.zero(pair.source))

    private val quote: Observable<TransferQuote>
        get() = quotesProvider.fetchQuote(direction = direction, pair = pair).flatMapObservable { quote ->
            val interval = min(quote.creationDate.diffInMillis(quote.expirationDate), MIN_QUOTE_REFRESH)
            quoteRefreshTimes.add(interval)
            Observable.interval(
                interval,
                interval,
                TimeUnit.MILLISECONDS
            ).flatMapSingle {
                quoteRefreshTimes.add(interval)
                quotesProvider.fetchQuote(direction = direction, pair = pair)
            }.startWithItem(quote)
        }.takeUntil(stop)

    private var pricedQuote: Observable<PricedQuote>? = null

    private val emitPerSecond = Observable.interval(TIMER_DELAY, TIMER_DELAY, TimeUnit.SECONDS).takeUntil(stop)

    fun getPricedQuote(): Observable<PricedQuote> {
        return pricedQuote?.let {
            it
        } ?: Observables.combineLatest(quote, amount, emitPerSecond).map { (quote, amount, emittedSeconds) ->
            val refreshTime = TimeUnit.MILLISECONDS.toSeconds(
                min(quote.creationDate.diffInMillis(quote.expirationDate), MIN_QUOTE_REFRESH)
            ).toInt()

            val elapsedSeconds = if (emittedSeconds < refreshTime) {
                emittedSeconds
            } else {
                val summedTimes = TimeUnit.MILLISECONDS.toSeconds(quoteRefreshTimes.dropLast(1).sum()).toInt()
                emittedSeconds - summedTimes
            }

            val remainingSeconds = refreshTime - elapsedSeconds
            val remainingPercentage = (remainingSeconds.toDouble() / refreshTime.toDouble())

            PricedQuote(
                PricesInterpolator(
                    list = quote.prices,
                    pair = pair
                ).getRate(amount),
                quote,
                remainingSeconds.toInt(),
                remainingPercentage.toFloat()
            )
        }.doFinally {
            pricedQuote = null
        }.doOnNext {
            latestQuote = it
        }.takeUntil(stop)
            .cache().also {
                pricedQuote = it
            }
    }

    fun start(
        direction: TransferDirection,
        pair: CurrencyPair
    ) {
        stop()
        this.direction = direction
        this.pair = pair
    }

    fun stop() {
        stop.onNext(Unit)
        quoteRefreshTimes.clear()
        pricedQuote = null
    }

    fun getLatestQuote(): PricedQuote = latestQuote

    fun updateAmount(newAmount: Money) = amount.onNext(newAmount)

    companion object {
        const val MIN_QUOTE_REFRESH: Long = 90000L
        private const val TIMER_DELAY: Long = 1
    }
}

data class PricedQuote(
    val price: Money,
    val transferQuote: TransferQuote,
    val remainingSeconds: Int,
    val remainingPercentage: Float
)

private fun Date.diffInMillis(other: Date): Long = (this.time - other.time).absoluteValue
