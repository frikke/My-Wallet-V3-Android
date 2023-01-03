package com.blockchain.coincore.impl

import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.PriceTier
import info.blockchain.balance.Money
import java.math.BigDecimal
import java.math.RoundingMode

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
