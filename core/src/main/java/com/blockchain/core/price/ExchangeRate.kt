package com.blockchain.core.price

import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import info.blockchain.balance.UnknownValue
import info.blockchain.balance.ValueTypeMismatchException
import java.math.BigDecimal
import java.math.RoundingMode

class ExchangeRate(
    private val rate: BigDecimal?,
    val from: Currency,
    val to: Currency
) {
    fun convert(value: Money, round: Boolean = true): Money {
        validateCurrency(from, value.currency)
        return rate?.let { rate ->
            Money.fromMajor(
                to,
                rate.multiply(value.toBigDecimal())
            )
        } ?: UnknownValue.unknownValue(to)
    }

    val price: Money
        get() = rate?.let {
            Money.fromMajor(to, it)
        } ?: UnknownValue.unknownValue(to)

    fun inverse(roundingMode: RoundingMode = RoundingMode.HALF_UP, scale: Int = -1): ExchangeRate {
        return ExchangeRate(
            from = to,
            to = from,
            rate = rate?.takeIf { it != BigDecimal.ZERO }?.let { rate ->
                BigDecimal.ONE.divide(
                    rate,
                    if (scale == -1) from.precisionDp else scale,
                    roundingMode
                ).stripTrailingZeros()
            } ?: rate
        )
    }

    companion object {
        private fun validateCurrency(expected: Currency, got: Currency) {
            if (expected != got)
                throw ValueTypeMismatchException("exchange", expected.networkTicker, got.networkTicker)
        }

        fun zeroRateExchangeRate(currency: Currency): ExchangeRate =
            ExchangeRate(
                rate = BigDecimal.ZERO,
                from = currency,
                to = currency
            )

        fun identityExchangeRate(currency: Currency): ExchangeRate =
            ExchangeRate(
                rate = BigDecimal.ONE,
                from = currency,
                to = currency
            )
    }
}

fun ExchangeRate.canConvert(value: Money): Boolean = this.from == value.currency
