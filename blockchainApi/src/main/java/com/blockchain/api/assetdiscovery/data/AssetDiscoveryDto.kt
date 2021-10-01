package com.blockchain.api.assetdiscovery.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class DynamicCurrency(
    @SerialName("symbol")
    private val symbol: String, // "ADA"
    @SerialName("displaySymbol")
    private val display: String? = null, // "ADA"
    @SerialName("name")
    val assetName: String,
    @SerialName("type")
    val coinType: AssetType,
    @SerialName("precision")
    val precision: Int,
    @SerialName("products")
    val products: List<String>
) {
    @Transient
    val networkSymbol: String = symbol
    @Transient
    val displaySymbol: String = display ?: symbol
}

@Serializable
internal sealed class AssetType {
    @SerialName("logoPngUrl")
    val logoUrl: String? = null

    @SerialName("websiteUrl")
    val websiteUrl: String? = null

    @Serializable
    @SerialName("COIN")
    data class L1CryptoAsset(
        @SerialName("minimumOnChainConfirmations")
        val minConfirmations: Int
    ) : AssetType()

    @Serializable
    @SerialName("ERC20")
    data class L2CryptoAsset(
        @SerialName("parentChain")
        val parentChain: String,
        @SerialName("erc20Address")
        val chainIdentifier: String
    ) : AssetType()

    @Serializable
    @SerialName("FIAT")
    data class FiatAsset(
        val unused: String? = null
    ) : AssetType()
}

@Serializable
internal data class DynamicCurrencyList(
    @SerialName("currencies")
    val currencies: List<DynamicCurrency>
)

/* internal */ class SupportedProduct {
    companion object {
        const val PRODUCT_PRIVATE_KEY = "PrivateKey"
    }
}
