package com.blockchain.nabu.datamanagers.repositories

import com.blockchain.core.custodial.models.BrokerageQuote
import com.blockchain.core.custodial.models.Promo
import com.blockchain.core.custodial.models.QuoteFee
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.trade.model.QuotePrice
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.nabu.datamanagers.PriceTier
import com.blockchain.nabu.models.responses.swap.QuoteRequest
import com.blockchain.nabu.service.NabuService
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.util.Date

class QuotesProvider(
    private val nabuService: NabuService,
) {

    fun getSampleDepositAddress(
        direction: TransferDirection,
        pair: CurrencyPair
    ): Single<String> = nabuService.fetchQuote(
        QuoteRequest(
            product = "BROKERAGE",
            direction = direction.toString(),
            pair = pair.rawValue
        )
    ).map { it.sampleDepositAddress }

    fun fetchPrice(
        amount: Money,
        direction: TransferDirection,
        pair: CurrencyPair
    ): Single<QuotePrice> =
        fetchQuote(amount, direction, pair).map {
            QuotePrice(
                currencyPair = pair,
                amount = it.inputAmount,
                price = it.price,
                rawPrice = it.rawPrice,
                resultAmount = it.resultAmount,
                dynamicFee = it.staticFee,
                networkFee = it.networkFee,
                paymentMethod = PaymentMethodType.FUNDS,
                orderProfileName = "",
            )
        }

    fun fetchQuote(
        amount: Money,
        direction: TransferDirection,
        pair: CurrencyPair
    ): Single<BrokerageQuote> =
        nabuService.fetchQuote(
            QuoteRequest(
                product = "BROKERAGE",
                direction = direction.toString(),
                pair = pair.rawValue
            )
        ).map {
            val prices = it.quote.priceTiers.map { price ->
                PriceTier(
                    volume = Money.fromMinor(pair.source, price.volume.toBigInteger()),
                    price = Money.fromMinor(pair.destination, price.price.toBigInteger())
                )
            }
            val price = PricesInterpolator(
                prices = prices,
                pair = pair
            ).getRate(amount)

            BrokerageQuote(
                id = it.id,
                currencyPair = pair,
                inputAmount = amount,
                price = price,
                rawPrice = price,
                // TODO(aromano): TEMP while brokerage/quote FF is on the code, resultAmount not currently being used
                resultAmount = Money.zero(pair.destination),
                quoteMargin = null,
                availability = null,
                settlementReason = null,
                networkFee = Money.fromMinor(pair.source, it.staticFee.toBigInteger()),
                staticFee = Money.fromMinor(pair.destination, it.networkFee.toBigInteger()),
                feeDetails = QuoteFee(
                    fee = Money.zero(pair.source),
                    feeBeforePromo = Money.zero(pair.source),
                    promo = Promo.NO_PROMO,
                ),
                createdAt = (it.createdAt.fromIso8601ToUtc()?.toLocalTime() ?: Date())
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli(),
                expiresAt = (it.expiresAt.fromIso8601ToUtc()?.toLocalTime() ?: Date())
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli(),
                depositTerms = null
            )
        }
}

class PricesInterpolator(
    private val interpolator: Interpolator = LinearInterpolator(),
    private val pair: CurrencyPair,
    private val prices: List<PriceTier>
) {

    fun getRate(amount: Money): Money {
        if (prices.isEmpty()) return Money.zero(pair.source)
        if (amount <= prices.first().volume) return prices.first().price
        if (amount >= prices.last().volume) return prices.last().price

        val priceTier = prices.find { priceTier ->
            amount <= priceTier.volume
        } ?: prices.last()
        val priceIndex = prices.indexOf(priceTier)

        val isInBetweenVolumes = priceTier.volume != amount
        if (!isInBetweenVolumes) return priceTier.price

        val prevTier = prices[priceIndex - 1]

        return Money.fromMajor(
            pair.destination,
            interpolator.interpolate(
                listOf(prevTier.volume.toBigDecimal(), priceTier.volume.toBigDecimal()),
                listOf(prevTier.price.toBigDecimal(), priceTier.price.toBigDecimal()),
                amount.toBigDecimal(),
                pair.destination.precisionDp
            )
        )
    }
}

class LinearInterpolator : Interpolator {

    override fun interpolate(x: List<BigDecimal>, y: List<BigDecimal>, xi: BigDecimal, scale: Int): BigDecimal {
        require(x.size == y.size) { "Should be same size" }
        require(x.size == 2) { "Should contain two points" }
        require(x.zipWithNext().all { it.first <= it.second }) { "$x Should be sorted" }
        require(xi >= x[0] && xi <= x[1]) { "$xi Should be between ${x[0]} and ${x[1]}" }
        return (((xi - x[0]) * (y[1] - y[0])).divide(x[1] - x[0], scale, RoundingMode.HALF_UP)) + y[0]
        // Formula：Y = ( ( X - X1 )( Y2 - Y1) / ( X2 - X1) ) + Y1
        // X1, Y1 = first value, X2, Y2 = second value, X = target value, Y = result
    }
}

interface Interpolator {
    fun interpolate(x: List<BigDecimal>, y: List<BigDecimal>, xi: BigDecimal, scale: Int): BigDecimal
}
