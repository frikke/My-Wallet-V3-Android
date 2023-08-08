package info.blockchain.balance

interface Currency {
    // Use for UI only.
    val displayTicker: String

    // If the ticker is NOT being directly displayed to the user in the UI, use this.
    val networkTicker: String
    val name: String
    val categories: Set<AssetCategory>

    // max decimal places; ie the quanta of this asset
    val precisionDp: Int
    val logo: String
    val colour: String
    val symbol: String

    // token price start times in epoch-seconds. null if charting not supported
    val startDate: Long?
    val type: CurrencyType

    val index: Int
        get() = DEFAULT_ASSET_ORDER_INDEX
}

enum class CurrencyType {
    CRYPTO, FIAT
}

fun Currency.asFiatCurrencyOrThrow(): FiatCurrency {
    return (this as? FiatCurrency)?.let {
        it
    } ?: throw IllegalArgumentException("Currency is $networkTicker of type $type is not a fiat currency")
}

fun Currency.asAssetInfoOrThrow(): AssetInfo {
    return (this as? AssetInfo) ?: throw IllegalArgumentException(
        "Currency is $networkTicker of type $type is not a asset info currency"
    )
}

val Currency.isCustodial: Boolean
    get() = categories.any {
        it == AssetCategory.TRADING ||
            it == AssetCategory.INTEREST
    }

private const val DEFAULT_ASSET_ORDER_INDEX = 0
