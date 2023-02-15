package info.blockchain.balance

import kotlinx.serialization.Serializable

@Serializable
data class CoinNetwork(
    val explorerUrl: String,
    val nativeAssetTicker: String,
    val networkTicker: String,
    val name: String,
    val shortName: String,
    val type: NetworkType,
    val chainId: Int?,
    val nodeUrls: List<String>,
    val feeCurrencies: List<String>,
    val isMemoSupported: Boolean
) : java.io.Serializable {
    val nodeUrl: String
        get() = nodeUrls.first()
}

// By having coerceInputValues set to true we can force the unknown values to be replaced by the default value
// of the property with this type. That way we can control which networks are enable and support.
enum class NetworkType {
    EVM,
    BTC,
    BCH,
    XLM,
    STX,
    SOL,
    NOT_SUPPORTED
}
