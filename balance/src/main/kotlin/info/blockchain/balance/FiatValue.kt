package info.blockchain.balance

import com.blockchain.utils.tryParseBigDecimal
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private data class Key(val locale: Locale, val currencyCode: String, val includeSymbol: Boolean)

private object FiatFormat {

    private val cache: MutableMap<Key, NumberFormat> = ConcurrentHashMap()

    operator fun get(key: Key) = cache.getOrPut(key) {
        val currencyInstance = Currency.getInstance(key.currencyCode)
        val fmt = NumberFormat.getCurrencyInstance(key.locale) as DecimalFormat
        fmt.apply {
            decimalFormatSymbols =
                decimalFormatSymbols.apply {
                    currency = currencyInstance
                    if (!key.includeSymbol) {
                        currencySymbol = ""
                    }
                }
            minimumFractionDigits = currencyInstance.defaultFractionDigits
            maximumFractionDigits = currencyInstance.defaultFractionDigits
            roundingMode = RoundingMode.DOWN
        }
    }
}

@Serializable
class FiatValue private constructor(
    override val currency: FiatCurrency,
    private val amount: @Contextual BigDecimal
) : Money() {

    // ALWAYS for display, so use default Locale
    override val symbol: String =
        currency.symbol

    override val maxDecimalPlaces: Int get() = maxDecimalPlaces(currencyCode)

    override val isZero: Boolean get() = amount.signum() == 0

    override val isPositive: Boolean get() = amount.signum() == 1

    override fun toBigDecimal(): BigDecimal = amount

    override fun toBigInteger(): BigInteger =
        amount.movePointRight(maxDecimalPlaces).toBigInteger()

    override fun toFloat(): Float =
        toBigDecimal().toFloat()

    override fun toStringWithSymbol(): String =
        FiatFormat[Key(Locale.getDefault(), currencyCode, includeSymbol = true)].format(amount)

    override fun toStringWithoutSymbol(): String =
        FiatFormat[Key(Locale.getDefault(), currencyCode, includeSymbol = false)]
            .format(amount)
            .trim()

    override fun toNetworkString(): String =
        FiatFormat[Key(Locale.US, currencyCode, includeSymbol = false)]
            .format(amount)
            .trim()
            .removeComma()

    override fun add(other: Money): FiatValue {
        require(other is FiatValue)
        return FiatValue(currency, amount + other.amount)
    }

    override fun subtract(other: Money): FiatValue {
        require(other is FiatValue)
        return FiatValue(currency, amount - other.amount)
    }

    override fun division(other: Money): Money {
        require(other is FiatValue)
        return FiatValue(currency, amount / other.amount)
    }

    override fun compare(other: Money): Int {
        require(other is FiatValue)
        return amount.compareTo(other.amount)
    }

    override fun ensureComparable(operation: String, other: Money) {
        if (other is FiatValue) {
            if (currencyCode != other.currencyCode) {
                throw ValueTypeMismatchException(operation, currencyCode, other.currencyCode)
            }
        } else {
            throw ValueTypeMismatchException(operation, currencyCode, other.currencyCode)
        }
    }

    override fun toZero(): FiatValue = fromMajor(currency, BigDecimal.ZERO)

    override fun equals(other: Any?): Boolean =
        (other is FiatValue) && (other.currencyCode == currencyCode) && (other.amount.compareTo(amount) == 0)

    override fun hashCode(): Int {
        var result = currencyCode.hashCode()
        result = 31 * result + amount.hashCode()
        return result
    }

    companion object {

        fun fromMinor(fiatCurrency: FiatCurrency, minor: BigInteger) =
            fromMajor(
                fiatCurrency,
                BigDecimal.valueOf(minor.toLong()).movePointLeft(fiatCurrency.precisionDp)
            )

        @JvmStatic
        fun fromMajor(fiatCurrency: FiatCurrency, major: BigDecimal, round: Boolean = true) =
            FiatValue(
                fiatCurrency,
                if (round) major.setScale(
                    fiatCurrency.precisionDp,
                    RoundingMode.DOWN
                ) else major
            )

        fun fromMajorOrZero(fiatCurrency: FiatCurrency, major: String, locale: Locale = Locale.getDefault()) =
            fromMajor(
                fiatCurrency,
                major.tryParseBigDecimal(locale) ?: BigDecimal.ZERO
            )

        fun zero(currency: FiatCurrency) = FiatValue(currency, BigDecimal.ZERO)

        private fun maxDecimalPlaces(currencyCode: String) =
            Currency.getInstance(currencyCode).defaultFractionDigits
    }
}
