package com.blockchain.domain.wallet

data class CoinNetwork(
    val explorerUrl: String,
    val currency: String,
    val network: String?,
    val name: String,
    val type: NetworkType,
    val chainId: Int?,
    val nodeUrls: List<String>,
    val feeCurrencies: List<String>,
    val isMemoSupported: Boolean
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
