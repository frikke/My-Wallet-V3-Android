package info.blockchain.balance

import java.lang.IllegalStateException
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Currency
import java.util.Locale

class UnknownValue private constructor(
    override val currencyCode: String,
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

    override fun toStringWithSymbol(): String = "--"

    override fun toStringWithoutSymbol(): String = "--"

    override fun toNetworkString(): String {
        throw IllegalStateException("Don't write UnknownValues to the network")
    }

    override fun toBigInteger(): BigInteger = BigInteger.ZERO
    override fun toBigDecimal(): BigDecimal = BigDecimal.ZERO
    override fun toFloat(): Float = 0.0F

    override fun ensureComparable(operation: String, other: Money) {
        if (other !is UnknownValue || other.currencyCode != currencyCode) {
            throw ValueTypeMismatchException(operation, currencyCode, other.currencyCode)
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
        fun unknownCryptoValue(asset: AssetInfo) =
            UnknownValue(
                currencyCode = asset.displayTicker,
                symbol = asset.displayTicker
            )

        fun unknownFiatValue(currencyCode: String) =
            UnknownValue(
                currencyCode = currencyCode,
                symbol = try {
                    Currency.getInstance(currencyCode).getSymbol(Locale.getDefault())
                } catch (t: IllegalArgumentException) {
                    throw IllegalArgumentException("${t.message} (currency=$currencyCode)")
                }
            )
    }
}
