package info.blockchain.balance

import java.lang.IllegalStateException
import java.math.BigDecimal
import java.math.BigInteger

class UnknownValue private constructor(
    override val currency: Currency,
    override val symbol: String
) : Money() {

    override val isZero: Boolean
        get() = true
    override val isPositive: Boolean
        get() = false
    override val maxDecimalPlaces: Int
        get() = 0

    override fun toZero(): Money {
        throw IllegalStateException("Cannot Zero unknown value")
    }

    override fun toStringWithSymbol(includeDecimalsWhenWhole: Boolean): String = "--"

    override fun toStringWithoutSymbol(): String = "--"

    override fun toNetworkString(): String {
        throw IllegalStateException("Don't write UnknownValues to the network")
    }

    override fun toBigInteger(): BigInteger = BigInteger.ZERO
    override fun toBigDecimal(): BigDecimal = BigDecimal.ZERO
    override fun toFloat(): Float = 0.0F

    override fun ensureComparable(operation: String, other: Money) {
        if (other !is UnknownValue || other.currency != currency) {
            throw ValueTypeMismatchException(operation, currency.networkTicker, other.currency.networkTicker)
        }
    }

    override fun add(other: Money): Money = this
    override fun subtract(other: Money): Money = this
    override fun division(other: Money): Money = this
    override fun compare(other: Money): Int {
        require(other is UnknownValue)
        require(other.currencyCode == currencyCode)
        return 0
    }

    companion object {
        fun unknownValue(currency: Currency) =
            UnknownValue(
                currency = currency,
                symbol = currency.displayTicker
            )
    }
}
