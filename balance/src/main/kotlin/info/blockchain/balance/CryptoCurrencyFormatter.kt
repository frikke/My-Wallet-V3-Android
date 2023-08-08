package info.blockchain.balance

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

enum class FormatPrecision {
    /**
     * Some currencies will be displayed at a shorter length
     */
    Short,

    /**
     * Full decimal place precision is used for the display string
     */
    Full
}

internal fun CryptoValue.format(
    locale: Locale,
    precision: FormatPrecision = FormatPrecision.Short
): String = getFormatter(locale).format(this, precision)

internal fun CryptoValue.formatWithUnit(
    locale: Locale,
    precision: FormatPrecision = FormatPrecision.Short,
    includeDecimalsWhenWhole: Boolean = true
) = getFormatter(locale).formatWithUnit(this, precision, includeDecimalsWhenWhole)

private val formatterMap: MutableMap<Locale, CryptoCurrencyFormatter> = ConcurrentHashMap()

private fun getFormatter(locale: Locale) =
    formatterMap.getOrPut(locale) { CryptoCurrencyFormatter(locale) }

private class CryptoCurrencyFormatter(private val locale: Locale) {

    fun format(
        cryptoValue: CryptoValue,
        precision: FormatPrecision = FormatPrecision.Short
    ): String = cryptoValue.currency.decimalFormat(cryptoValue, precision)
        .formatWithoutUnit(cryptoValue.toBigDecimal())

    fun formatWithUnit(
        cryptoValue: CryptoValue,
        precision: FormatPrecision = FormatPrecision.Short,
        includeDecimalsWhenWhole: Boolean
    ) = cryptoValue.currency.decimalFormat(cryptoValue, precision, includeDecimalsWhenWhole).formatWithUnit(
        cryptoValue.toBigDecimal(),
        cryptoValue.currency.displayTicker
    )

    private fun AssetInfo.decimalFormat(
        value: Money,
        displayMode: FormatPrecision,
        includeDecimalsWhenWhole: Boolean = true
    ) = createCryptoDecimalFormat(
        locale,
        when {
            includeDecimalsWhenWhole || !value.valueIsWholeNumber() -> {
                if (displayMode == FormatPrecision.Short) {
                    CryptoValue.DISPLAY_DP
                } else {
                    this.precisionDp
                }
            }
            else -> {
                0
            }
        }
    )

    private fun DecimalFormat.formatWithUnit(value: BigDecimal, symbol: String) =
        "${formatWithoutUnit(value)} $symbol"

    private fun DecimalFormat.formatWithoutUnit(value: BigDecimal) =
        format(value.toPositiveDouble()).toWebZero()
}

private fun BigDecimal.toPositiveDouble() = this.toDouble().toPositiveDouble()

private fun Double.toPositiveDouble() = abs(this)

/**
 * Replace 0.0 with 0 to match web
 */
private fun String.toWebZero() = if (this == "0.0" || this == "0,0" || this == "0.00") "0" else this

private fun createCryptoDecimalFormat(locale: Locale, maxDigits: Int, minDigits: Int = 1) =
    (NumberFormat.getInstance(locale) as DecimalFormat).apply {
        minimumFractionDigits = minDigits
        maximumFractionDigits = maxDigits
        roundingMode = RoundingMode.DOWN
    }
