package info.blockchain.balance

import java.io.Serializable
import java.util.Locale

@kotlinx.serialization.Serializable
data class FiatCurrency private constructor(
    private val currencyCode: String
) : Currency, Serializable {
    override val displayTicker: String
        get() = currencyCode
    override val networkTicker: String
        get() = currencyCode
    override val name: String
        get() = java.util.Currency.getInstance(currencyCode).getDisplayName(Locale.getDefault())
    override val categories: Set<AssetCategory>
        get() = setOf(AssetCategory.TRADING)
    override val precisionDp: Int
        get() = java.util.Currency.getInstance(currencyCode).defaultFractionDigits
    override val logo: String
        get() = "file:///android_asset/logo/${currencyCode.lowercase(Locale.ROOT)}/logo.png"
    override val colour: String
        get() = "#FF00B26B"
    override val startDate: Long?
        get() = null
    override val symbol: String
        get() = java.util.Currency.getInstance(currencyCode).getSymbol(Locale.getDefault())

    override val type: CurrencyType
        get() = CurrencyType.FIAT

    fun tickerWithSymbol(): String =
        "$displayTicker ($symbol)"

    fun nameWithSymbol(): String =
        "$name ($symbol)"

    companion object {
        fun fromCurrencyCode(currencyCode: String): FiatCurrency =
            FiatCurrency(currencyCode)

        fun locale(): FiatCurrency = try {
            FiatCurrency(java.util.Currency.getInstance(Locale.getDefault()).currencyCode)
        } catch (e: Exception) {
            Dollars
        }

        val Dollars: FiatCurrency = fromCurrencyCode("USD")
    }

    override fun equals(other: Any?): Boolean {
        return (other as? FiatCurrency)?.networkTicker == networkTicker
    }

    override fun hashCode(): Int {
        return 31 * 17 + currencyCode.hashCode()
    }

    override fun toString(): String {
        return displayTicker
    }
}
