package com.blockchain.api.coinnetworks.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoinNetworkResponse(
    @SerialName("networks")
    val networks: List<CoinNetwork>,
    @SerialName("types")
    val types: List<CoinType>
)

@Serializable
data class CoinNetwork(
    @SerialName("explorerUrl")
    val explorerUrl: String,
    @SerialName("nativeAsset")
    val currency: String,
    @SerialName("networkTicker")
    val network: String? = null,
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: NetworkType = NetworkType.NOT_SUPPORTED,
    @SerialName("identifiers")
    val identifiers: Identifiers,
    @SerialName("nodeUrls")
    val nodeUrls: List<String>,
    @SerialName("feeCurrencies")
    val feeCurrencies: List<String>,
    @SerialName("memos")
    val isMemoSupported: Boolean
)

@Serializable
data class Identifiers(
    @SerialName("chainId")
    val chainId: Int? = null
)

@Serializable
data class CoinType(
    @SerialName("type")
    val type: NetworkType = NetworkType.NOT_SUPPORTED,
    @SerialName("derivations")
    val derivations: List<Derivation>,
    @SerialName("style")
    val style: STYLE
)

@Serializable
data class Derivation(
    @SerialName("purpose")
    val purpose: Int,
    @SerialName("coinType")
    val coinType: Int,
    @SerialName("descriptor")
    val descriptor: Int
)

// By having coerceInputValues set to true we can force the unknown values to be replaced by the default value
// of the property with this type. That way we can control which networks are enable and support.
enum class NetworkType {
    EVM,
    BTC,
    XLM,
    NOT_SUPPORTED
}

enum class STYLE {
    SINGLE,
    EXTENDED
}
