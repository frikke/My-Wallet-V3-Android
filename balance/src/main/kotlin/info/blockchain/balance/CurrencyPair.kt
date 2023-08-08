package info.blockchain.balance

data class CurrencyPair(val source: Currency, val destination: Currency) {

    val rawValue: String
        get() = listOf(source.networkTicker, destination.networkTicker).joinToString("-")

    companion object {
        fun fromRawPair(
            rawValue: String,
            assetCatalogue: AssetCatalogue
        ): CurrencyPair? {
            val parts = rawValue.split("-")
            val source: Currency = assetCatalogue.fromNetworkTicker(parts[0]) ?: return null
            val destination: Currency = assetCatalogue.fromNetworkTicker(parts[1]) ?: return null
            return CurrencyPair(source, destination)
        }
    }
}
