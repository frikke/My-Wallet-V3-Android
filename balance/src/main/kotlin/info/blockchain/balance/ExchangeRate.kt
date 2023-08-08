package info.blockchain.balance

import java.math.BigDecimal
import java.math.RoundingMode

class ExchangeRate(
    private val rate: BigDecimal?,
    val from: Currency,
    val to: Currency
) {
    fun convert(value: Money, roundingMode: RoundingMode? = null): Money {
        validateCurrency(from, value.currency)
        return rate?.let { rate ->
            val result = if (roundingMode != null) {
                rate.multiply(value.toBigDecimal()).setScale(to.precisionDp, roundingMode)
            } else {
                rate.multiply(value.toBigDecimal())
            }
            Money.fromMajor(
                to,
                result
            )
        } ?: UnknownValue.unknownValue(to)
    }

    override fun equals(other: Any?): Boolean {
        return (other is ExchangeRate) &&
            (other.rate == rate) &&
            (other.from.networkTicker == from.networkTicker) &&
            (other.to.networkTicker == to.networkTicker)
    }

    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + rate.hashCode()
        result = 31 * result + from.networkTicker.hashCode()
        result = 31 * result + to.networkTicker.hashCode()
        return result
    }

    val price: Money
        get() = rate?.let {
            Money.fromMajor(to, it)
        } ?: UnknownValue.unknownValue(to)

    fun inverse(roundingMode: RoundingMode = RoundingMode.HALF_UP, scale: Int = -1): ExchangeRate {
        return ExchangeRate(
            from = to,
            to = from,
            rate = rate?.takeIf { it.signum() != 0 }?.let { rate ->
                BigDecimal.ONE.divide(
                    rate,
                    if (scale == -1) from.precisionDp + to.precisionDp else scale,
                    roundingMode
                ).stripTrailingZeros()
            } ?: rate
        )
    }

    override fun toString(): String {
        return "ExchangeRate(from=${from.networkTicker}, to=${to.networkTicker}, rate=$rate)"
    }

    companion object {
        private fun validateCurrency(expected: Currency, got: Currency) {
            if (expected != got) {
                throw ValueTypeMismatchException("exchange", expected.networkTicker, got.networkTicker)
            }
        }

        fun zeroRateExchangeRate(from: Currency, to: Currency = from): ExchangeRate =
            ExchangeRate(
                rate = BigDecimal.ZERO,
                from = from,
                to = to
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
