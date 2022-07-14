package info.blockchain.balance

import java.io.Serializable
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.Locale

abstract class Money : Serializable {

    abstract val currency: Currency

    // User displayable symbol
    abstract val symbol: String

    val currencyCode: String
        get() = currency.networkTicker

    abstract val isZero: Boolean
    abstract val isPositive: Boolean

    abstract val maxDecimalPlaces: Int

    /**
     * Where a Money type can store more decimal places than is necessary,
     * this property can be used to limit it for user input and display.
     */
    open val userDecimalPlaces: Int
        get() = maxDecimalPlaces

    abstract fun toZero(): Money

    // Format for display
    /**
     * includeDecimalsWhenWhole is by default set to true so in case
     * amount value is a whole number then trailing zeros are returned normally.
     * Set this flag to false, in order to get only the integer part in the case above .
     */
    abstract fun toStringWithSymbol(includeDecimalsWhenWhole: Boolean = true): String
    abstract fun toStringWithoutSymbol(): String

    // Format for network transmission
    abstract fun toNetworkString(): String

    // Type conversions
    abstract fun toBigInteger(): BigInteger
    abstract fun toBigDecimal(): BigDecimal
    abstract fun toFloat(): Float

    /**
     * The formatted string in parts in the specified locale, or the systems default locale.
     */
    fun toStringParts() =
        toStringWithoutSymbol().let {
            val index = it.lastIndexOf(LocaleDecimalFormat[Locale.getDefault()].decimalFormatSymbols.decimalSeparator)
            if (index != -1) {
                Parts(
                    symbol = symbol,
                    major = it.substring(0, index),
                    minor = it.substring(index + 1),
                    majorAndMinor = it
                )
            } else {
                Parts(
                    symbol = symbol,
                    major = it,
                    minor = "",
                    majorAndMinor = it
                )
            }
        }

    class Parts(
        val symbol: String,
        val major: String,
        val minor: String,
        val majorAndMinor: String
    )

    fun formatOrSymbolForZero() =
        if (isZero) {
            symbol
        } else {
            toStringWithSymbol()
        }

    operator fun plus(other: Money): Money {
        ensureComparable("add", other)
        return add(other)
    }

    operator fun minus(other: Money): Money {
        ensureComparable("subtract", other)
        return subtract(other)
    }

    operator fun div(other: Money): Money {
        ensureComparable("division", other)
        return division(other)
    }

    operator fun compareTo(other: Money): Int {
        ensureComparable("compare", other)
        return compare(other)
    }

    internal abstract fun ensureComparable(operation: String, other: Money)
    protected abstract fun add(other: Money): Money
    protected abstract fun subtract(other: Money): Money
    protected abstract fun division(other: Money): Money
    protected abstract fun compare(other: Money): Int

    companion object {
        fun min(a: Money, b: Money): Money {
            a.ensureComparable("compare", b)
            return if (a <= b) a else b
        }

        fun max(a: Money, b: Money): Money {
            a.ensureComparable("compare", b)
            return if (a >= b) a else b
        }

        fun fromMinor(currency: Currency, value: BigInteger): Money =
            when (currency) {
                is CryptoCurrency -> CryptoValue.fromMinor(currency, value)
                is FiatCurrency -> FiatValue.fromMinor(currency, value)
                else -> throw IllegalArgumentException("Unsupported type")
            }

        fun fromMajor(currency: Currency, value: BigDecimal): Money =
            when (currency) {
                is CryptoCurrency -> CryptoValue.fromMajor(currency, value)
                is FiatCurrency -> FiatValue.fromMajor(currency, value)
                else -> throw IllegalArgumentException("Unsupported type")
            }

        fun zero(currency: Currency): Money = fromMinor(currency, BigInteger.ZERO)
    }
}

fun Money?.percentageDelta(previous: Money?): Double =
    if (this != null && previous != null && !previous.isZero) {
        val current = this.toBigDecimal()
        val prev = previous.toBigDecimal()

        (current - prev)
            .divide(prev, 4, RoundingMode.HALF_EVEN)
            .movePointRight(2)
            .toDouble()
    } else {
        Double.NaN
    }

fun Iterable<Money>.total(): Money {
    if (!iterator().hasNext())
        throw IndexOutOfBoundsException("Can't sum an empty list")
    return reduce { a, v -> a + v }
}

open class ValueTypeMismatchException(
    verb: String,
    lhsSymbol: String,
    rhsSymbol: String
) : RuntimeException("Can't $verb $lhsSymbol and $rhsSymbol")

fun String.removeComma(): String {
    return replace(",", "")
}

fun Money.valueIsWholeNumber() =
    toBigDecimal().rem(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0
